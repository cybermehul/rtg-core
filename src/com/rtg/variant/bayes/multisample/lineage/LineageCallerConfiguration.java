/*
 * Copyright (c) 2014. Real Time Genomics Limited.
 *
 * Use of this source code is bound by the Real Time Genomics Limited Software Licence Agreement
 * for Academic Non-commercial Research Purposes only.
 *
 * If you did not receive a license accompanying this file, a copy must first be obtained by email
 * from support@realtimegenomics.com.  On downloading, using and/or continuing to use this source
 * code you accept the terms of that license agreement and any amendments to those terms that may
 * be made from time to time by Real Time Genomics Limited.
 */
package com.rtg.variant.bayes.multisample.lineage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.rtg.reference.Ploidy;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.Relationship;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.variant.MachineErrorChooserInterface;
import com.rtg.reference.SexMemo;
import com.rtg.variant.VariantParams;
import com.rtg.variant.bayes.complex.DenovoChecker;
import com.rtg.variant.bayes.complex.LineageDenovoChecker;
import com.rtg.variant.bayes.multisample.AbstractJointCallerConfiguration;
import com.rtg.variant.bayes.multisample.IndividualSampleFactory;
import com.rtg.variant.bayes.multisample.JointCallerConfigurator;
import com.rtg.variant.bayes.multisample.MultisampleUtils;
import com.rtg.variant.bayes.multisample.Utils;
import com.rtg.variant.bayes.multisample.population.PopulationHwHypothesesCreator;
import com.rtg.variant.bayes.snp.ModelNoneFactory;
import com.rtg.variant.bayes.snp.ModelSnpFactory;

/**
 */
public final class LineageCallerConfiguration extends AbstractJointCallerConfiguration {

  /**
   * The factory for this caller.
   */
  public static final class Configurator implements JointCallerConfigurator {

    /**
     * Create a new family joint caller
     * @param params parameters
     * @param outputGenomes the genomes that will have output
     * @throws java.io.IOException if error
     * @return a new {@link com.rtg.variant.bayes.multisample.lineage.LineageCallerConfiguration}
     */
    @Override
    public LineageCallerConfiguration getConfig(final VariantParams params, String[] outputGenomes) throws IOException {
      Diagnostic.userLog("Using Lineage caller");

      final List<String> genomes = new ArrayList<>();
      genomes.addAll(Arrays.asList(outputGenomes));
      final int[] parents = new int[outputGenomes.length];

      final GenomeRelationships genomeRelationships = params.genomeRelationships();
      if (outputGenomes.length != genomeRelationships.genomes().length) {
        throw new NoTalkbackSlimException("Number of samples in pedigree and number of output genomes don't match");
      }
      final Lineage.LineageBuilder builder = new Lineage.LineageBuilder();
      final boolean noRelationships = genomeRelationships.relationships(Relationship.RelationshipType.PARENT_CHILD).length < 1;
      if (noRelationships) {
        throw new NoTalkbackSlimException("Lineage requires at least one parent child relationship");
      }
      for (Relationship r : genomeRelationships.relationships(Relationship.RelationshipType.PARENT_CHILD)) {
        final int childPos = genomes.indexOf(r.second());
        parents[childPos]++;
        final int parentPos = genomes.indexOf(r.first());
        builder.add(parentPos, childPos);
      }
      for (int i = 0; i < parents.length; i++) {
        if (parents[i] > 1) {
          throw new NoTalkbackSlimException("Lineage requires at most one parent per individual, sample '" + outputGenomes[i] + "' had " + parents[i]);
        }
      }

      if (outputGenomes.length == 0) {
        throw new NoTalkbackSlimException("VCF output for lineage calling needs SAM headers with sample names");
      }

      final MachineErrorChooserInterface chooser = MultisampleUtils.chooser(params);
      final PopulationHwHypothesesCreator ssp;
      if (params.populationPriorFile() != null) {
        ssp = new PopulationHwHypothesesCreator(params.populationPriorFile(), params.genomePriors(), params.referenceRanges());
      } else {
        ssp = null;
      }
      builder.deNovoPriorDefault(params.genomePriors().denovoRef()).params(params).coverage(false);
      final Lineage caller = builder.create();
      final ModelSnpFactory haploid = new ModelSnpFactory(params.genomePriors(), true);
      final ModelSnpFactory diploid = new ModelSnpFactory(params.genomePriors(), false);
      final ModelNoneFactory none = new ModelNoneFactory();
      final List<IndividualSampleFactory<?>> individualFactories = new ArrayList<>();
      final SexMemo sexMemo = Utils.createSexMemo(params);
      for (String genome : genomes) {
        individualFactories.add(new IndividualSampleFactory<>(params, chooser, haploid, diploid, none, genomeRelationships.getSex(genome), sexMemo));
      }
      return new LineageCallerConfiguration(caller, outputGenomes, individualFactories, chooser, haploid, diploid, ssp);
    }
  }




  LineageDenovoChecker mCorrector;
  private LineageCallerConfiguration(Lineage jointCaller, String[] genomeNames, List<IndividualSampleFactory<?>> individualFactories, MachineErrorChooserInterface machineErrorChooser, ModelSnpFactory haploid, ModelSnpFactory diploid, PopulationHwHypothesesCreator ssp) {
    super(jointCaller, genomeNames, individualFactories, machineErrorChooser, haploid, diploid, ssp);
    mCorrector = new LineageDenovoChecker(jointCaller);
  }

  @Override
  public DenovoChecker getDenovoCorrector() {
    return mCorrector;
  }

  @Override
  public boolean handlesPloidy(final Ploidy ploidy) {
    return ploidy != Ploidy.POLYPLOID;
  }
}
