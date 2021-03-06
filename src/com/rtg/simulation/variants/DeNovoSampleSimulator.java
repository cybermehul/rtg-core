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
package com.rtg.simulation.variants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rtg.reader.SequencesReader;
import com.rtg.reference.Ploidy;
import com.rtg.reference.ReferenceGenome;
import com.rtg.reference.ReferenceGenome.DefaultFallback;
import com.rtg.reference.ReferenceSequence;
import com.rtg.reference.Sex;
import com.rtg.relation.GenomeRelationships;
import com.rtg.relation.VcfPedigreeParser;
import com.rtg.util.PortableRandom;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.io.FileUtils;
import com.rtg.variant.GenomePriorParams;
import com.rtg.variant.VariantStatistics;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 * Creates a genotyped sample as the same as an input sample with the addition of de novo mutations.
 *
 */
public class DeNovoSampleSimulator {

  private final SequencesReader mReference;
  private final PortableRandom mRandom;
  private final DefaultFallback mDefaultPloidy;
  private final PriorPopulationVariantGenerator mGenerator;
  private VariantStatistics mStats = null;
  private int mOriginalSampleNum = -1;
  private Sex[] mOriginalSexes = null;
  private ReferenceGenome mOriginalRefg = null;
  private ReferenceGenome mMaleGenome = null;
  private ReferenceGenome mFemaleGenome = null;
  private boolean mVerbose = true;

  /**
   * @param reference input reference data
   * @param params genome params
   * @param rand random number generator
   * @param ploidy the default ploidy to use if no reference specification is present
   * @param targetMutations expected number of mutations per genome
   * @param verbose if true output extra information on crossover points
   * @throws IOException if an I/O error occurs.
   */
  public DeNovoSampleSimulator(SequencesReader reference, GenomePriorParams params, PortableRandom rand, DefaultFallback ploidy, final int targetMutations, boolean verbose) throws IOException {
    mReference = reference;
    mRandom = rand;
    mDefaultPloidy = ploidy;
    mVerbose = verbose;
    mGenerator = new PriorPopulationVariantGenerator(reference, new PopulationMutatorPriors(params), rand, new PriorPopulationVariantGenerator.FixedAlleleFrequencyChooser(1.0), targetMutations);
  }

  /**
   * Create a genotyped sample using population variants defined in file.
   * @param vcfInFile input population data. requires allele frequencies
   * @param vcfOutFile destination of sample genotype
   * @param origSample name of the father sample
   * @param sample name to give the generated sample
   * @throws java.io.IOException if an IO error occurs
   */
  public void mutateIndividual(File vcfInFile, File vcfOutFile, String origSample, String sample) throws IOException {
    final VcfHeader header = VcfUtils.getHeader(vcfInFile);
    if (header.getSampleNames().contains(sample)) {
      throw new NoTalkbackSlimException("sample '" + sample + "' already exists");
    }
    mOriginalSampleNum = header.getSampleNames().indexOf(origSample);
    if (mOriginalSampleNum == -1) {
      throw new NoTalkbackSlimException("original sample '" + origSample + "' does not exist");
    }

    final GenomeRelationships ped = VcfPedigreeParser.load(header);
    mOriginalSexes = new Sex[ped.genomes().length];
    for (String genome : header.getSampleNames()) {
      mOriginalSexes[header.getSampleNames().indexOf(genome)] = ped.getSex(genome);
    }
    final Sex originalSex = ped.getSex(origSample);
    mOriginalRefg = new ReferenceGenome(mReference, originalSex, mDefaultPloidy);
    mMaleGenome = new ReferenceGenome(mReference, Sex.MALE, mDefaultPloidy);
    mFemaleGenome = new ReferenceGenome(mReference, Sex.FEMALE, mDefaultPloidy);

    boolean foundGt = false;
    boolean foundDeNovo = false;
    for (FormatField ff : header.getFormatLines()) {
      if (ff.getId().equals(VcfUtils.FORMAT_GENOTYPE)) {
        foundGt = true;
      } else if (ff.getId().equals(VcfUtils.FORMAT_DENOVO)) {
        foundDeNovo = true;
      }
    }
    if (!foundGt) {
      throw new NoTalkbackSlimException("input VCF does not contain GT information");
    }
    if (!foundDeNovo) {
      header.addFormatField(VcfUtils.FORMAT_DENOVO, MetaType.STRING, new VcfNumber("1"), "De novo allele");
    }
    if (mVerbose) {
      Diagnostic.info("Original ID=" + origSample + " Sex=" + originalSex);
    }

    header.addSampleName(sample);
    if (originalSex == Sex.FEMALE || originalSex == Sex.MALE) {
      header.addLine(VcfHeader.SAMPLE_STRING + "=<ID=" + sample + ",Sex=" + originalSex.toString() + ">");
    }
    header.addLine(VcfHeader.PEDIGREE_STRING + "=<Derived=" + sample + ",Original=" + origSample + ">");

    header.addRunInfo();
    header.addLine(VcfHeader.META_STRING + "SEED=" + mRandom.getSeed());

    mStats = new VariantStatistics(null);
    mStats.onlySamples(sample);
    try (VcfWriter vcfOut = new VcfWriter(header, vcfOutFile, null, FileUtils.isGzipFilename(vcfOutFile), true)) {
      final ReferenceGenome refG = new ReferenceGenome(mReference, originalSex, mDefaultPloidy);

      // Generate de novo variants
      final List<PopulationVariantGenerator.PopulationVariant> deNovo = mGenerator.generatePopulation();
      for (long i = 0; i < mReference.numberSequences(); i++) {
        final ReferenceSequence refSeq = refG.sequence(mReference.name(i));

        final List<PopulationVariantGenerator.PopulationVariant> seqDeNovo = new LinkedList<>();
        for (PopulationVariantGenerator.PopulationVariant v : deNovo) {
          if (v.getSequenceId() == i) {
            seqDeNovo.add(v);
          }
        }

        outputSequence(vcfInFile, vcfOut, refSeq, seqDeNovo);
      }
    }
    if (mVerbose) {
      Diagnostic.info(""); // Just to separate the statistics
    }
    Diagnostic.info(mStats.getStatistics());
  }

  // Treat polyploid as haploid
  private int getEffectivePloidy(int ploidy) {
    return ploidy == -1 ? 1 : ploidy;
  }

  //writes sample to given writer, returns records as list
  private List<VcfRecord> outputSequence(File vcfPopFile, VcfWriter vcfOut, ReferenceSequence refSeq, List<PopulationVariantGenerator.PopulationVariant> deNovo) throws IOException {
    final Ploidy ploidy = mOriginalRefg.sequence(refSeq.name()).ploidy();
    final int ploidyCount = getEffectivePloidy(ploidy.count());
    final int sampleId = vcfOut.getHeader().getNumberOfSamples() - 1;

    final String desc = "Original=" + ploidy + " -> Derived=" + ploidy;
    if (ploidyCount > 2) {
      throw new NoTalkbackSlimException("Sequence " + refSeq.name() + ": Unsupported ploidy" + desc);
    }
    if ((ploidyCount == 0) && (deNovo.size() > 0)) {
      Diagnostic.developerLog("Ignoring " + deNovo.size() + " deNovo variants generated for chromosome " + refSeq.name() + " with ploidy NONE");
      deNovo.clear();
    }
    //System.err.println("Sequence " + refSeq.name() + " has ploidy " + desc);
    final ArrayList<VcfRecord> sequenceVariants = new ArrayList<>();
    try (VcfReader reader = VcfReader.openVcfReader(vcfPopFile, new RegionRestriction(refSeq.name()))) {
      while (reader.hasNext()) {
        final VcfRecord v = reader.next();
        final int nextVariantPos = v.getStart();

        outputDeNovo(refSeq, deNovo, nextVariantPos, ploidyCount, sampleId, vcfOut, sequenceVariants);

        // For non de-novo, append sample genotype from original genotype
        v.setNumberOfSamples(sampleId + 1);
        final String gt = v.getFormatAndSample().get(VcfUtils.FORMAT_GENOTYPE).get(mOriginalSampleNum);
        for (String format : v.getFormats()) {
          final String value = format.equals(VcfUtils.FORMAT_GENOTYPE) ? gt : VcfRecord.MISSING;
          v.addFormatAndSample(format, value);
        }

        sequenceVariants.add(v);
        vcfOut.write(v);
        mStats.tallyVariant(vcfOut.getHeader(), v);
      }

      // Output any remaining de novo variants
      outputDeNovo(refSeq, deNovo, Integer.MAX_VALUE, ploidyCount, sampleId, vcfOut, sequenceVariants);

    }
    return sequenceVariants;
  }

  private void outputDeNovo(ReferenceSequence refSeq, List<PopulationVariantGenerator.PopulationVariant> deNovo, final int endPos, final int ploidyCount, final int sampleId, VcfWriter vcfOut, final ArrayList<VcfRecord> sequenceVariants) throws IOException {
    // Merge input and de novo, requires records to be sorted
    while (!deNovo.isEmpty() && (deNovo.get(0).getStart() <= endPos)) {
      final PopulationVariantGenerator.PopulationVariant pv = deNovo.remove(0);
      final VcfRecord dv = pv.toVcfRecord(mReference);
      if (dv.getStart() == endPos) {
        // We could be smarter and merge the records, but this will be so infrequent I am not going to bother
        if (mVerbose) {
          Diagnostic.info("Skipping De Novo mutation at " + refSeq.name() + ":" + dv.getOneBasedStart() + " to avoid collision with existing variant.");
        }
        continue;
      }
      dv.getInfo().clear(); // To remove AF chosen by the population variant generator
      if (mVerbose) {
        Diagnostic.info("De Novo mutation at " + refSeq.name() + ":" + dv.getOneBasedStart());
      }
      addSamplesForDeNovo(dv, sampleId, ploidyCount, refSeq.name());

      sequenceVariants.add(dv);
      vcfOut.write(dv);
      mStats.tallyVariant(vcfOut.getHeader(), dv);
    }
  }


  private String addSamplesForDeNovo(final VcfRecord v, final int sampleId, final int ploidyCount, String refName) {
    final String sampleGt = ploidyCount == 1 ? "1" : mRandom.nextBoolean() ? "0|1" : "1|0";
    v.addFormat(VcfUtils.FORMAT_GENOTYPE);
    v.addFormat(VcfUtils.FORMAT_DENOVO);
    v.setNumberOfSamples(sampleId + 1);
    for (int id = 0; id <= sampleId; id++) {
      final String gt;
      if (id != sampleId) {
        final ReferenceGenome sampleGenome = mOriginalSexes[id] == Sex.MALE ? mMaleGenome : mFemaleGenome;
        final Ploidy ploidy = sampleGenome.sequence(refName).ploidy();
        switch (ploidy) {
          case HAPLOID:
            gt = "0";
            break;
          case DIPLOID:
            gt = "0|0";
            break;
          default:
            gt = ".";
            break;
        }
      } else {
        gt = sampleGt;
      }

      v.addFormatAndSample(VcfUtils.FORMAT_GENOTYPE, gt);
      v.addFormatAndSample(VcfUtils.FORMAT_DENOVO, (id == sampleId) ? "Y" : "N");
    }
    return sampleGt;
  }

}
