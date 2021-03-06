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
package com.rtg.variant;

import java.io.File;
import java.io.IOException;

import com.rtg.calibrate.CalibrationStats;
import com.rtg.calibrate.Calibrator;
import com.rtg.calibrate.Covariate;
import com.rtg.calibrate.CovariateEnum;
import com.rtg.util.Histogram;
import com.rtg.util.MathUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.realign.RealignParams;
import com.rtg.variant.realign.RealignParamsImplementation;
import com.rtg.variant.util.VariantUtils;

/**
 * Implementation that uses calibration files.
 */
public class CalibratedMachineErrorParams extends AbstractMachineErrorParams {

  //any phred score lower than 2 theoretically means that all other options are more likely than the observed value.
  private static final int MIN_PHRED_SCORE = 2;

  //  private static final boolean QUE_TEN_HACK = false; //Boolean.valueOf(System.getProperty("cg-q10hack", "false"));
  private static final boolean GAP_HACK = false; //Boolean.valueOf(System.getProperty("cg-gaphack", "false"));
  //private static final int[] QUE_TEN_HACK_CURVE = {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};

  private static final double[] CG_DEFAULT_OVERLAP_DIST = {0.0, 0.08, 0.84, 0.08, 0.0};
  private static final double[] CG_DEFAULT_SMALL_GAP_DIST = {0.9696969696969697, 0.020202020202020204, 0.010101010101010102, 0.0};

  private static final int QUE_HACK_LIMIT = 255; //use 255 if to effectively ignore
  //  static {
  //    try {
  //      QUE_HACK_LIMIT = Integer.parseInt(System.getProperty("q-limit", "255"));
  //    } catch (final NumberFormatException e) {
  //      throw new RuntimeException(e);
  //    }
  //  }

  private final PhredScaler mScaler;
  private final MachineType mMachineType;

  private final double[] mErrorDelDist;
  private final double[] mErrorInsDist;
  private final double[] mErrorMnpDist;
  private final double[] mErrorGapDist;
  private final double[] mErrorSmallGapDist;
  private final double[] mErrorOverlapDist;

  private final double mDelBaseRate;
  private final double mInsBaseRate;
  private final double mMnpBaseRate;
  private final double mDelEventRate;
  private final double mInsEventRate;
  private final double mMnpEventRate;

  private final RealignParams mRealignParams;

  /**
   * Constructor
   * @param mt machine type for read group
   * @param calibrator calibrator for all read groups
   * @param readGroupId read group to use for this error parameters.
   */
  public CalibratedMachineErrorParams(MachineType mt, Calibrator calibrator, String readGroupId) {
    mMachineType = mt;

    final Histogram nhHist = calibrator.getHistogram(Calibrator.NH_DIST, readGroupId);
    if (nhHist == null) {
      throw new NoTalkbackSlimException("No calibration data supplied for read group: " + readGroupId);
    } else if (nhHist.getLength() == 0) {
      throw new NoTalkbackSlimException("Calibration data indicates no coverage for the read group: " + readGroupId);
    }
    final Histogram delHist = calibrator.getHistogram(Calibrator.DEL_DIST, readGroupId);
    mErrorDelDist = delHist != null && delHist.getLength() > 1 ? delHist.toDistribution() : new double[] {0.0, 1.0};
    final Histogram insHist = calibrator.getHistogram(Calibrator.INS_DIST, readGroupId);
    mErrorInsDist = insHist != null && insHist.getLength() > 1 ? insHist.toDistribution() : new double[] {0.0, 1.0};
    final Histogram mnpHist = calibrator.getHistogram(Calibrator.MNP_DIST, readGroupId);
    mErrorMnpDist = mnpHist != null && mnpHist.getLength() > 1 ? mnpHist.toDistribution() : new double[] {0.0, 1.0};

    if (isCG()) {
      if (GAP_HACK) {
        Diagnostic.developerLog("CG gap hack enabled, using default gaps");
        mErrorGapDist = new double[] {0.0, 0.28, 0.6, 0.12, 0.0};
        mErrorSmallGapDist = CG_DEFAULT_SMALL_GAP_DIST;
        mErrorOverlapDist = CG_DEFAULT_OVERLAP_DIST;
      } else if (calibrator.hasHistogram(Calibrator.CGGAP_DIST, readGroupId)) {
        final Histogram gapH = calibrator.getHistogram(Calibrator.CGGAP_DIST, readGroupId);
        //overlap can be all 0s if we are parsing an old-style CG sam file, in which case use default overlap.
        final Histogram olapH;
        if (calibrator.hasHistogram(Calibrator.CGOVER_DIST, readGroupId)) {
          olapH = calibrator.getHistogram(Calibrator.CGOVER_DIST, readGroupId);
        } else {
          olapH = null;
        }
        final double[][] dists = histogramsToCgDists(gapH, olapH);
        mErrorGapDist = dists[0];
        mErrorSmallGapDist = dists[1];
        mErrorOverlapDist = dists[2];
      } else {
        Diagnostic.developerLog("Calibration file without CG gap information, using default gaps");
        mErrorGapDist = new double[] {0.0, 0.28, 0.6, 0.12, 0.0};
        mErrorSmallGapDist = CG_DEFAULT_SMALL_GAP_DIST;
        mErrorOverlapDist = CG_DEFAULT_OVERLAP_DIST;
      }
    } else {
      mErrorGapDist = new double[5];
      mErrorSmallGapDist = new double[4];
      mErrorOverlapDist = new double[5];
    }

    final CalibrationStats sums = calibrator.getSums(CovariateEnum.READGROUP, readGroupId);
    mMnpBaseRate = (double) sums.getDifferent() / (sums.getEqual() + sums.getDifferent() + 1);
    mInsBaseRate = (double) sums.getInserted() / (sums.getEqual() + sums.getDifferent() + 1);
    mDelBaseRate = (double) sums.getDeleted() / (sums.getEqual() + sums.getDifferent() + 1);

    mMnpEventRate = mMnpBaseRate / GenomePriorParamsBuilder.averageLength(mErrorMnpDist);
    mInsEventRate = mInsBaseRate / GenomePriorParamsBuilder.averageLength(mErrorInsDist);
    mDelEventRate = mDelBaseRate / GenomePriorParamsBuilder.averageLength(mErrorDelDist);

    //    if (mMachineType == MachineType.COMPLETE_GENOMICS && QUE_TEN_HACK) {
    //      Diagnostic.developerLog("CG q10 hack enabled, using q10 scaler");
    //      mScaler = getCgQueTenHackScaler(readGroupId);
    //    } else {
    mScaler = getScaler(calibrator, readGroupId);
    //    }
    mRealignParams = new RealignParamsImplementation(this);
  }

  /*private static PhredScaler getCgQueTenHackScaler(final String readGroup) {
    Diagnostic.developerLog("Using q10 hack quality curve for read group: " + readGroup + " : " + Arrays.toString(QUE_TEN_HACK_CURVE));
    return new PhredScaler() {
      @Override
      public int getPhred(char qualChar, int readPosition) {
        final int q = qualChar - '!';
        if (q >= QUE_TEN_HACK_CURVE.length) {
          return QUE_TEN_HACK_CURVE[QUE_TEN_HACK_CURVE.length - 1];
        }
        return QUE_TEN_HACK_CURVE[q];
      }
    };
  }*/

  private static PhredScaler getScaler(Calibrator cal, String readGroup) {
    if (cal.getCovariateIndex(CovariateEnum.BASEQUALITY) == -1) {
      throw new IllegalArgumentException("Base quality covariate currently required");
    }
    final Calibrator.QuerySpec query = cal.initQuery();
    final int readGroupIndex = cal.getCovariateIndex(CovariateEnum.READGROUP);
    if (readGroupIndex >= 0) {
      query.setValue(CovariateEnum.READGROUP, cal.getCovariate(readGroupIndex).parse(readGroup));
    }
    if (cal.getCovariateIndex(CovariateEnum.READPOSITION) != -1) {
      return new BaseQualityReadPositionPhredScaler(cal, query);
    } else {
      return new BaseQualityPhredScaler(cal, query);
    }
  }

  static int countsToEmpiricalQuality(long mismatches, long totalReadPositions, double globalErrorRate) {
    final double error = (mismatches + globalErrorRate) / (totalReadPositions + 1.0);
    return Math.max((int) MathUtils.round(MathUtils.phred(error)), MIN_PHRED_SCORE);
  }

  /**
   * produce distributions for machine error parameters
   * @param gapHistogram histogram with index equal to gap length
   * @param overlapHistogram histogram with index equal to overlap length
   * @return the three distributions (index 0 for gap distribution, index 1 for small gap distribution, index 2 for overlap distribution)
   */
  static double[][] histogramsToCgDists(Histogram gapHistogram, Histogram overlapHistogram) {
    //evil mojo to shunt histograms into non-standard yet expected array sizes and orientation
    //each read must have 1 and only 1 gap of between 4 and 8 (incl) long
    long totalReads = 0;
    for (int i = 4; i < gapHistogram.getLength(); i++) {
      totalReads += gapHistogram.getValue(i);
    }

    //gap distribution expected with index 0 being probability of gap length 4 and index 4 being gap length 8;
    final double[] gapDist = new double[5];
    for (int i = 0; i < gapDist.length && 4 + i < gapHistogram.getLength(); i++) {
      gapDist[i] = (double) gapHistogram.getValue(4 + i) / totalReads;
    }

    //each read must have 1 and only 1 gap of between 0 and 3 (incl) long, but we can't count those with 0 long gaps
    int totSmallGaps = 0;
    for (int i = 0; i < gapHistogram.getLength() && i < 4; i++) {
      totSmallGaps += gapHistogram.getValue(i);
    }
    if (totSmallGaps > totalReads) {
      throw new IllegalArgumentException("invalid CG gap distribution, small gaps (1-3) has larger count than big gaps(4-8)");
    }
    if (gapHistogram.getLength() > 9) {
      throw new IllegalArgumentException("invalid CG gap distribution");
    }

    //small gaps in slightly easier form of entry being probabilty of gap with length of its index
    double[] smallGapDist = new double[4];
    long totalSmall = 0;
    for (int i = 1; i < smallGapDist.length && i < gapHistogram.getLength(); i++) {
      smallGapDist[i] = (double) gapHistogram.getValue(i) / totalReads;
      totalSmall += gapHistogram.getValue(i);
    }
    if (totalSmall == 0) {
      smallGapDist = CG_DEFAULT_SMALL_GAP_DIST;
    } else {
      //infer number of 0 long gaps from total reads and counted small gaps
      smallGapDist[0] = (double) (totalReads - totSmallGaps) / (double) totalReads;
    }

    final double[] overlapDist;
    if (overlapHistogram != null) {
      //each read must have 1 and only 1 overlap of between 0 and 4 (incl) long, again we can't keep track of 0 long whilst counting
      long totalOverlap = 0;
      for (int i = 1; i < overlapHistogram.getLength(); i++) {
        totalOverlap += overlapHistogram.getValue(i);
      }
      if (totalOverlap > totalReads) {
        throw new IllegalArgumentException("invalid CG overlap distribution, more overlap than big gaps");
      }
      if (overlapHistogram.getLength() > 5) {
        throw new IllegalArgumentException("invalid CG overlap distribution");
      }

      //machine error params expects overlap distribution in wierd form with index 0 referring to probabilty of gap of 4 long and index 4 referring to probability of gap of 0 long
      overlapDist = new double[5];
      for (int i = 1; i < overlapHistogram.getLength(); i++) {
        overlapDist[4 - i] = (double) overlapHistogram.getValue(i) / totalReads;
      }
      //again use total to infer number of 0 long overlaps
      overlapDist[4] = (double) (totalReads - totalOverlap) / (double) totalReads;
    } else {
      overlapDist = CG_DEFAULT_OVERLAP_DIST;
    }
    return new double[][] {gapDist, smallGapDist, overlapDist};
  }

  @Override
  public boolean cgTrimOuterBases() {
    return isCG() && MachineErrorParamsBuilder.CG_TRIM;
  }

  @Override
  public double errorDelBaseRate() {
    return mDelBaseRate;
  }

  @Override
  public double[] errorDelDistribution() {
    return mErrorDelDist;
  }

  @Override
  public double errorDelEventRate() {
    return mDelEventRate;
  }

  @Override
  public double errorInsBaseRate() {
    return mInsBaseRate;
  }

  @Override
  public double[] errorInsDistribution() {
    return mErrorInsDist;
  }

  @Override
  public double errorInsEventRate() {
    return mInsEventRate;
  }

  @Override
  public double[] errorMnpDistribution() {
    return mErrorMnpDist;
  }

  @Override
  public double errorMnpEventRate() {
    return mMnpEventRate;
  }

  @Override
  public double errorSnpRate() {
    return mMnpBaseRate;
  }

  @Override
  public double[] gapDistribution() {
    return mErrorGapDist;
  }


  @Override
  public double[] smallGapDistribution() {
    return mErrorSmallGapDist;
  }

  @Override
  public double[] overlapDistribution() {
    return mErrorOverlapDist;
  }

  @Override
  public int getPhred(char qualChar, int readPos) {
    return Math.min(QUE_HACK_LIMIT, mScaler.getPhred(qualChar, readPos));
  }

  @Override
  public final boolean isCG() {
    return mMachineType == MachineType.COMPLETE_GENOMICS;
  }

  @Override
  public MachineType machineType() {
    return mMachineType;
  }


  @Override
  protected int[] qualityCurve() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public RealignParams realignParams() {
    return mRealignParams;
  }

  private static final String DUMP_MERGED = "dump-merged";
  private static final String DUMP_PARAMS = "dump-params";

  /**
   * Test from command line
   * @param args command line arguments
   * @throws IOException if the calibration files cannot be read
   */
  public static void main(String... args) throws IOException {
    final CFlags flags = new CFlags();
    flags.registerOptional(DUMP_MERGED, "if set, output merged calibration file");
    flags.registerOptional(DUMP_PARAMS, "if set, output machine error params");
    flags.registerRequired(File.class, "FILE", "SAM file containing unmapped reads to attempt to rescue").setMaxCount(Integer.MAX_VALUE);
    flags.setFlags(args);
    Diagnostic.setLogStream();
    Covariate[] covariates = null;
    for (final Object o : flags.getAnonymousValues(0)) {
      covariates = Calibrator.getCovariateSet((File) o);
    }
    if (covariates == null) {
      throw new IllegalStateException("no calibration covariates found");
    }
    final Calibrator c = new Calibrator(covariates, null);
    for (final Object o : flags.getAnonymousValues(0)) {
      c.accumulate((File) o);
    }
    if (flags.isSet(DUMP_MERGED)) {
      c.writeToStream(System.out);
    }
    if (flags.isSet(DUMP_PARAMS)) {
      final Covariate rgc = c.getCovariate(c.getCovariateIndex(CovariateEnum.READGROUP));
      for (int rgid = 0; rgid < rgc.size(); rgid++) {
        System.out.println("##############################################");
        final String rgname = rgc.valueString(rgid);
        final CalibratedMachineErrorParams cme = new CalibratedMachineErrorParams(null, c, rgname);
        System.out.println("# Machine errors for read group: " + rgname);
        System.out.println(VariantUtils.toMachineErrorProperties(cme));
      }
    }
  }
}
