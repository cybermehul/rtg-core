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
package com.rtg.variant.avr;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.CommonFlags;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.CommonFlagCategories;
import com.rtg.util.cli.Flag;
import com.rtg.util.cli.Validator;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.vcf.VcfReader;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.VcfWriter;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class PredictCli extends AbstractCli {

  protected static final String INPUT_FLAG = "input";
  protected static final String OUTPUT_FLAG = "output";
  protected static final String SAMPLE_FLAG = "sample";

  @Override
  protected void initFlags() {
    CommonFlagCategories.setCategories(mFlags);
    CommonFlags.initNoGzip(mFlags);
    CommonFlags.initIndexFlags(mFlags);
    mFlags.setDescription("Use an AVR model to re-score variants in a VCF file.");
    mFlags.registerRequired('i', INPUT_FLAG, File.class, "FILE", "input VCF file").setCategory(CommonFlagCategories.INPUT_OUTPUT);
    mFlags.registerRequired('o', OUTPUT_FLAG, File.class, "FILE", "output VCF file").setCategory(CommonFlagCategories.INPUT_OUTPUT);

    final Flag avrFlag = CommonFlags.initAvrModel(mFlags, false);
    if (avrFlag.getParameterDefault() == null) {
      avrFlag.setMinCount(1); // Make required if no default available
    }

    CommonFlags.initMinAvrScore(mFlags);

    mFlags.registerOptional('s', SAMPLE_FLAG, String.class, "STRING", "if set, only re-score the specified samples (Default is to re-score all samples)").setCategory(CommonFlagCategories.UTILITY).setMaxCount(Integer.MAX_VALUE);
    mFlags.setValidator(new Validator() {
      @Override
      public boolean isValid(CFlags flags) {
        final File o = (File) flags.getValue(OUTPUT_FLAG);
        if (!CommonFlags.isStdio(o)) {
          final File output = FileUtils.getZippedFileName(!flags.isSet(CommonFlags.NO_GZIP), o);
          if (output.exists()) {
            flags.setParseMessage("The file \"" + output + "\" already exists. Please remove it first or choose a different file");
            return false;
          }
        }
        return true;
      }
    });
  }

  @Override
  protected int mainExec(OutputStream out, PrintStream err) throws IOException {
    final File modelFile = CommonFlags.getAvrModel(mFlags, false);
    final double threshold = mFlags.isSet(CommonFlags.FILTER_AVR_FLAG) ? (Double) mFlags.getValue(CommonFlags.FILTER_AVR_FLAG) : 0;
    if (modelFile == null) {
      throw new NoTalkbackSlimException("No model file specified and no default model available.");
    }
    final ModelFactory fact = new ModelFactory(modelFile, threshold);
    final AbstractPredictModel model = fact.getModel();
    final File vcf = (File) mFlags.getValue(INPUT_FLAG);

    try (final VcfReader posReader = VcfReader.openVcfReader(vcf)) {
      final VcfHeader header = posReader.getHeader();
      header.addRunInfo();
      model.updateHeader(header);

      final List<Object> samplesList = mFlags.getValues(SAMPLE_FLAG);
      final int[] samples = new int[samplesList.size()];
      for (int i = 0; i < samples.length; i++) {
        final Integer sampleIndex = header.getSampleIndex((String) samplesList.get(i));
        if (sampleIndex == null) {
          throw new NoTalkbackSlimException("The sample name \"" + samplesList.get(i) + "\" is not present in the input VCF file");
        }
        samples[i] = sampleIndex;
      }

      final File o = (File) mFlags.getValue(OUTPUT_FLAG);
      final boolean stdout = CommonFlags.isStdio(o);
      final boolean gzip = !mFlags.isSet(CommonFlags.NO_GZIP);
      final boolean index = !mFlags.isSet(CommonFlags.NO_INDEX);
      final File vcfFile = stdout ? null : FileUtils.getZippedFileName(gzip, o);
      try (VcfWriter writer = new VcfWriter(header, vcfFile, out, gzip, index)) {
        while (posReader.hasNext()) {
          final VcfRecord current = posReader.next();
          if (samples.length > 0) {
            for (final int s : samples) {
              model.annotateSample(current, s);
            }
          } else {
            model.annotate(current);
          }
          writer.write(current);
        }
      }
    }

    return 0;
  }

  @Override
  public String moduleName() {
    return "avrpredict";
  }
}
