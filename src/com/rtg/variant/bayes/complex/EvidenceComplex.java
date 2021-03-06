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

package com.rtg.variant.bayes.complex;

import com.rtg.mode.DNA;
import com.rtg.sam.SamUtils;
import com.rtg.util.MaxShiftUtils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;
import com.rtg.util.machine.MachineType;
import com.rtg.variant.AbstractMachineErrorParams;
import com.rtg.variant.MachineErrorChooserInterface;
import com.rtg.variant.ThreadingEnvironment;
import com.rtg.variant.VariantAlignmentRecord;
import com.rtg.variant.VariantParams;
import com.rtg.variant.bayes.Evidence;
import com.rtg.variant.bayes.EvidenceInterface;
import com.rtg.variant.bayes.Factor;
import com.rtg.variant.bayes.snp.HypothesesPrior;
import com.rtg.variant.match.AlignmentMatch;
import com.rtg.variant.realign.AlignmentEnvironment;
import com.rtg.variant.realign.AlignmentEnvironmentCG;
import com.rtg.variant.realign.AlignmentEnvironmentGenomeSubstitution;
import com.rtg.variant.realign.AlignmentEnvironmentRead;
import com.rtg.variant.realign.AllPaths;
import com.rtg.variant.realign.Environment;
import com.rtg.variant.realign.EnvironmentCombined;
import com.rtg.variant.realign.InvertCgTemplateEnvironment;
import com.rtg.variant.realign.RealignParams;
import com.rtg.variant.realign.ScoreFastUnderflow;
import com.rtg.variant.realign.ScoreFastUnderflowCG;
import com.rtg.variant.util.arithmetic.PossibilityArithmetic;

/**
 * Provides evidence for a complex hypothesis by performing an all-paths alignment
 * from an alignment record to each hypothesis.
 *
 */
public class EvidenceComplex extends Evidence {

  /** print complex evidence scores in for debugging */
  private static final boolean PRINT_EVIDENCE_DETAILS = false; //Boolean.valueOf(System.getProperty("rtg.print-cx-evidence-details", "false"));

  private static final ScoreInterfaceMemoInterface SCORE_INTERFACE_MEMO;
  static {
    SCORE_INTERFACE_MEMO = new ScoreInterfaceMemo();
  }

  private static AllPaths getAllPaths(final RealignParams rp, final boolean isCG) {
    if (isCG) {
      return new ScoreFastUnderflowCG(rp);
    }
    return new ScoreFastUnderflow(rp);
  }

  private final int mReference;

  private final double[] mProb;

  private double mError = 0.0;

  private double mPE;

  private final Factor<DescriptionComplex> mHypotheses;

  private final int mReadHypothesis;

  private final int mReadBasesLeftOfMatch;
  private final int mReadBasesRightOfMatch;

  private final PossibilityArithmetic mArithmetic;

  private final double mLogSum;

  private static volatile boolean sIsIonTorrent = false;

  private final AlignmentMatch mMatch;

  // This is not very nice
  private static synchronized void printIonTorrentDevLog() {
    if (!sIsIonTorrent) {
      Diagnostic.developerLog("Ion Torrent machine type - widening allpaths");
      sIsIonTorrent = true;
    }
  }

  private final boolean mForward;
  private final boolean mReadPaired;
  private final boolean mMated;
  private final boolean mUnmapped;

  /**
   * @param hypotheses description of the underlying hypotheses.
   * @param match Match object to get distribution for
   * @param reference complex reference
   * @param params variant params
   * @param chooser machine error chooser
   */
  public EvidenceComplex(HypothesesPrior<DescriptionComplex> hypotheses, AlignmentMatch match, ComplexTemplate reference, VariantParams params, MachineErrorChooserInterface chooser) {
    super(hypotheses.description(), match.mapError());
    mHypotheses = hypotheses;
    mReference = hypotheses.reference();
    mArithmetic = hypotheses.arithmetic();
    mPE = mArithmetic.zero();
    mMatch = match;
    mReadBasesLeftOfMatch = match.getBasesLeftOfMatch();
    mReadBasesRightOfMatch = match.getBasesRightOfMatch();
    final int size = description().size();
    mProb = new double[size];
    final VariantAlignmentRecord alignmentRecord = match.alignmentRecord();
    final AbstractMachineErrorParams me = chooser.machineErrors(alignmentRecord);
    final boolean cg = me.isCG();
    final AllPaths sm;
    if (params.threadingEnvironment() == ThreadingEnvironment.PARALLEL) {
      sm = getAllPaths(me.realignParams(), cg);
    } else {
      sm = SCORE_INTERFACE_MEMO.getScoreInterface(me.realignParams(), cg);
    }

    final AlignmentEnvironment se;
    if (cg) {
      se = new AlignmentEnvironmentCG(alignmentRecord, params, me, reference.templateBytes());
    } else {
      se = new AlignmentEnvironmentRead(alignmentRecord, params, me);
    }
    final int maxShift0;
    final int newStart;
    int softClipStartOffset = 0;
    if (me.machineType() == MachineType.IONTORRENT) {
      final MaxShiftCigarParser calc = new MaxShiftCigarParser();
      calc.parse(alignmentRecord.getCigar(), alignmentRecord.getStart());
      maxShift0 = calc.getMaxShift();
      newStart = calc.getStartPos();
      softClipStartOffset = calc.getSoftClipStartOffset();
      if (!sIsIonTorrent) {
        printIonTorrentDevLog();
      }
    } else {
      maxShift0 = MaxShiftUtils.calculateDefaultMaxShift(se.subsequenceLength());
      newStart = alignmentRecord.getStart();

      final String cigar = match.alignmentRecord().getCigar();
      for (int i = 0, n = 0; i < cigar.length(); i++) {
        final char c = cigar.charAt(i);
        if (Character.isDigit(c)) {
          n = 10 * n + c - '0';
        } else if (c == SamUtils.CIGAR_SOFT_CLIP) {
          softClipStartOffset = n;
        } else {
          break;
        }
      }
    }
    //if (softClipStartOffset > 0) {
    //  System.err.println("EvidenceComplex with soft clipped start: " + alignmentRecord.getStart() + " " + alignmentRecord.getEnd() + " " + alignmentRecord.getCigar());
    //}
    final int adjust = hypotheses.description().maxLength() - hypotheses.description().minLength();
    assert adjust >= 0;
//    System.err.println("adjust=" + adjust + " maxShift0=" + maxShift0);

    final int maxShift = maxShift0 + adjust;
    int readHyp = EvidenceInterface.NULL;
    final double[] logScore = new double[size];
    double sum = mArithmetic.zero();
    for (int i = 0; i < size; i++) {
      //TODO put in fast delta scoring for non-CG case.
      final String replace = description().name(i);
      final AlignmentEnvironment temEnv = new AlignmentEnvironmentGenomeSubstitution(se.start() - softClipStartOffset, 0 /* doesn't matter */, reference, DNA.stringDNAtoByte(replace));
      final EnvironmentCombined envTmp = new EnvironmentCombined(se, newStart - softClipStartOffset, maxShift, temEnv);
      final Environment env;
      if (cg && !se.cgOverlapOnLeft()) {
        env = new InvertCgTemplateEnvironment(envTmp);
      } else {
        env = envTmp;
      }
      sm.setEnv(env);
      final double poss = mArithmetic.ln2Poss(sm.totalScoreLn());
      //System.err.println("Read match=" + match.readString() + " Hyp i=" + i + " name=" + hypotheses.description().name(i) + " : unnorm score=" + mArithmetic.poss2Ln(poss) + " scorematrix=\n" + sm.toString());
      logScore[i] = poss;
      sum = mArithmetic.add(sum, poss);
      if (hypotheses.description().match(i).readString().equals(match.readString())) {
        readHyp = i;
      }
    }

    //normalize
    mLogSum = mArithmetic.poss2Ln(sum);
    for (int i = 0; i < size; i++) {
      final double poss = mArithmetic.divide(logScore[i], sum);
      if (PRINT_EVIDENCE_DETAILS) {
        Diagnostic.developerLog("Read match=" + match.readString() + " Hyp i=" + i + " name=" + hypotheses.description().name(i) + " : score=" + mArithmetic.poss2Prob(poss));
      }
      set(i, poss);
    }
    mReadHypothesis = readHyp;

    mForward = !match.alignmentRecord().isNegativeStrand();
    mReadPaired = match.alignmentRecord().isReadPaired(); // True if is a paired end read at all, false if single end
    mMated = match.alignmentRecord().isMated(); // True if is a paired end read and is properly mated, false if single end or unmated
    mUnmapped = match.alignmentRecord().isUnmapped();
  }

  AlignmentMatch match() {
    return mMatch;
  }

  /**
   * Get the raw unnormalized sum in log probability.
   * Handy for unit tests.
   * @return the log sum.
   */
  double sumLn() {
    return mLogSum;
  }

  /**
   * Set the distribution value for a specified hypothesis.
   * @param index selects the (haploid) hypothesis.
   * @param poss probability to be assigned (in possibility arithmetic).
   */
  private void set(final int index, final double poss) {
    assert mArithmetic.isValidPoss(poss);
    mProb[index] = mArithmetic.poss2Prob(poss);
    //    System.err.println(index + ":" + mArithmetic.poss2Prob(poss) + ":" + mArithmetic.poss2Prob(mHypotheses.prior(index)));
    final double hypPrior = mHypotheses.p(index);
    assert mArithmetic.isValidPoss(hypPrior);
    mPE = mArithmetic.add(mPE, mArithmetic.multiply(hypPrior, poss));
    assert mArithmetic.isValidPoss(mPE);
    if (index != mReference) {
      mError += mProb[index];
    }
  }

  @Override
  public double probability(int index) {
    return mProb[index];
  }

  @Override
  public double pe() {
    return mArithmetic.poss2Prob(mPE);
  }

  @Override
  public int read() {
    return mReadHypothesis;
  }

  @Override
  public int getReadBasesLeft() {
    return mReadBasesLeftOfMatch;
  }

  @Override
  public int getReadBasesRight() {
    return mReadBasesRightOfMatch;
  }

  @Override
  public void setReadBasesLeft(int readBasesLeft) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setReadBasesRight(int readBaseRight) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertEquals(description().size(), mProb.length);
    return true;
  }

  @Override
  public double error() {
    return mError;
  }

  @Override
  public boolean isForward() {
    return mForward;
  }

  @Override
  public boolean isReadPaired() {
    return mReadPaired;
  }

  @Override
  public boolean isMated() {
    return mMated;
  }

  @Override
  public boolean isUnmapped() {
    return mUnmapped;
  }
}
