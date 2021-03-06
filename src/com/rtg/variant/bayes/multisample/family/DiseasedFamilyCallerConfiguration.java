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

package com.rtg.variant.bayes.multisample.family;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.rtg.reference.Sex;
import com.rtg.relation.Family;
import com.rtg.relation.GenomeRelationships;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.variant.MachineErrorChooserInterface;
import com.rtg.reference.SexMemo;
import com.rtg.variant.VariantParams;
import com.rtg.variant.bayes.multisample.AbstractJointCallerConfiguration;
import com.rtg.variant.bayes.multisample.IndividualSampleFactory;
import com.rtg.variant.bayes.multisample.JointCallerConfigurator;
import com.rtg.variant.bayes.multisample.MultisampleJointCaller;
import com.rtg.variant.bayes.multisample.MultisampleUtils;
import com.rtg.variant.bayes.multisample.Utils;
import com.rtg.variant.bayes.multisample.population.PopulationHwHypothesesCreator;
import com.rtg.variant.bayes.snp.ModelNoneFactory;
import com.rtg.variant.bayes.snp.ModelSnpFactory;
import com.rtg.variant.format.VariantOutputVcfFormatter;
import com.rtg.variant.format.VcfInfoField;

/**
 */
public final class DiseasedFamilyCallerConfiguration extends AbstractJointCallerConfiguration {

  /**
   * The factory for this caller.
   */
  public static final class Configurator implements JointCallerConfigurator {

    /**
     * Create a new disease joint caller
     * @param params parameters
     * @param outputGenomes names of genomes to produce calls for
     * @return a new {@link DiseasedFamilyCallerConfiguration}
     * @throws IOException if error
     */
    @Override
    public DiseasedFamilyCallerConfiguration getConfig(final VariantParams params, String[] outputGenomes) throws IOException {
      Diagnostic.userLog("Using disease caller");

      final GenomeRelationships genomeRelationships = params.genomeRelationships();
      final Family family = Family.getFamily(genomeRelationships);
      final String fatherName = family.getFather();
      final String motherName = family.getMother();
      final String[] genomes = new String[family.getChildren().length + Family.FIRST_CHILD_INDEX];
      final Sex[] sexes = new Sex[genomes.length];
      final List<String> childNames = new ArrayList<>();
      childNames.addAll(Arrays.asList(family.getChildren()));
      if (outputGenomes.length != genomes.length) {
        throw new NoTalkbackSlimException("Exactly " + genomes.length + " sample names expected in mappings");
      }
      genomes[Family.FATHER_INDEX] = fatherName;
      genomes[Family.MOTHER_INDEX] = motherName;
      sexes[Family.FATHER_INDEX] = genomeRelationships.getSex(fatherName);
      sexes[Family.MOTHER_INDEX] = genomeRelationships.getSex(motherName);
      Diagnostic.developerLog("First parent: " + fatherName + " sex: " + sexes[Family.FATHER_INDEX]);
      Diagnostic.developerLog("Second parent: " + motherName + " sex: " + sexes[Family.MOTHER_INDEX]);
      for (int i = 0; i < childNames.size(); i++) {
        final String childName = childNames.get(i);
        genomes[Family.FIRST_CHILD_INDEX + i] = childName;
        sexes[Family.FIRST_CHILD_INDEX + i] = genomeRelationships.getSex(childName);
        Diagnostic.developerLog("Child " + i + ": " + childName + " sex: " + sexes[Family.FIRST_CHILD_INDEX + i]);
      }
      final List<String> familyNames = Arrays.asList(genomes);
      for (String mapName : outputGenomes) {
        if (!familyNames.contains(mapName)) {
          throw new NoTalkbackSlimException("Unexpected sample name in mappings: " + mapName);
        }
      }
      final MachineErrorChooserInterface chooser = MultisampleUtils.chooser(params);
      final DiseasedFamilyCaller jointCaller = new DiseasedFamilyCaller(params, family, params.noDiseasePrior());
      final PopulationHwHypothesesCreator ssp;
      if (params.populationPriorFile() != null) {
        ssp = new PopulationHwHypothesesCreator(params.populationPriorFile(), params.genomePriors(), params.referenceRanges());
      } else {
        ssp = null;
      }
      final ModelSnpFactory haploid = new ModelSnpFactory(params.genomePriors(), true);
      final ModelSnpFactory diploid = new ModelSnpFactory(params.genomePriors(), false);
      final ModelNoneFactory none = new ModelNoneFactory();
      final List<IndividualSampleFactory<?>> individualFactories = new ArrayList<>();
      final SexMemo sexMemo = Utils.createSexMemo(params);
      for (int i = 0; i < genomes.length; i++) {
        individualFactories.add(new IndividualSampleFactory<>(params, chooser, haploid, diploid, none, sexes[i], sexMemo));
      }
      return new DiseasedFamilyCallerConfiguration(jointCaller, genomes, individualFactories, chooser, haploid, diploid, ssp);
    }
  }

  private DiseasedFamilyCallerConfiguration(MultisampleJointCaller jointCaller, String[] genomeNames, List<IndividualSampleFactory<?>> individualFactories, MachineErrorChooserInterface machineErrorChooser, ModelSnpFactory haploid, ModelSnpFactory diploid, PopulationHwHypothesesCreator ssp) {
    super(jointCaller, genomeNames, individualFactories, machineErrorChooser, haploid, diploid, ssp);
  }

  @Override
  public VariantOutputVcfFormatter getOutputFormatter(VariantParams params) {
    final VariantOutputVcfFormatter f = new VariantOutputVcfFormatter(params, getGenomeNames());
    f.addExtraInfoFields(EnumSet.of(VcfInfoField.DISEASE, VcfInfoField.RDS));
    return f;
  }
}
