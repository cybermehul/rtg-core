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

package com.rtg.variant.realign;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.DNA;
import com.rtg.util.StringUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.SpyCounter;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.IntegralAbstract;
import com.rtg.variant.util.arithmetic.PossibilityArithmetic;

/**
 * Calculates a banded matrix of alignment probabilities.
 * This is similar to the <code>Gotoh</code> algorithm, but calculates the
 * probability of all paths to a given point, rather than just the best path.
 *
 * This class and all its subclasses are capable of using several different
 * representations of probabilities, for example, native doubles (0.0 to 1.0)
 * or natural logs.  We use the term 'Possibility' for one of these encoded
 * probability values, so all variables whose name ends with 'Poss' should
 * only be manipulated via one of the possibility methods.
 *
 */
@TestClass("com.rtg.variant.realign.ScoreMatrixTest")
public abstract class AbstractAllPaths extends IntegralAbstract implements AllPaths {

  private static final SpyCounter SET_ENV = new SpyCounter("ScoreMatrix setEnv");
  private static final SpyCounter SET_ENV_RESIZE = new SpyCounter("ScoreMatrix setEnv resize");
  /** width of each number in the <code>toString</code> output */
  static final int FIELD_WIDTH = 7;

  protected final RealignParams mParams;
  protected final PossibilityArithmetic mArith;

  // some constant/parameter probabilities.
  protected final double mZeroPoss;
  protected final double mOnePoss;
  protected final double mOneInFourPoss;
  protected final double mMatchPoss;
  protected final double mMisMatchPoss;
  protected final double mDeleteOpenPoss;
  protected final double mDeleteExtendPoss;
  protected final double mInsertOpenPoss;
  protected final double mInsertExtendPoss;
  protected final double mOneMinusDeleteExtendPoss;
  protected final double mOneMinusDeleteOpenPoss;
  protected final double mOneMinusInsertExtendPoss;
  protected final double mOneMinusDeleteInsertOpenPoss;
  protected final double mDeleteOpenInFourPoss;

  private final double mDelOpen;


  /** The height of the matrix less 1 (so valid rows are from 0 .. <code>mLength</code> inclusive) */
  protected int mLength;

  /** The minimum width of the matrix (i.e., the width of the first row) */
  protected int mWidth;

  protected int mMaxWidth;  //the current full width of the matrix
  protected int mMaxLength;  //the current full length of the matrix

  protected Environment mEnv;
  protected double mDeleteStartPoss;
  protected double mMatchStartPoss;

  private double[][] mMatch;  // Possibility values
  private double[][] mInsert; // Possibility values
  private double[][] mDelete; // Possibility values

  /**
   * Cumulative sums along the 'final' row (which is the first row for a reverse matrix).
   * These sums are the Possibilities that the read ends after
   * the given position.
   */
  protected double[] mEndScores;

  /**
   * A score matrix with the given maximum band width.
   * @param arith helper object that does the arithmetic so that this code can be independent of the representation.
   * @param params the machine error model and related parameters.
   */
  protected AbstractAllPaths(final PossibilityArithmetic arith, final RealignParams params) {
    mArith = arith;
    //System.err.println(env.toString());
    mParams = params;
    mZeroPoss = mArith.zero();
    mOneInFourPoss = mArith.prob2Poss(0.25);
    mOnePoss = mArith.one();

    mMatchPoss = mArith.ln2Poss(mParams.matchLn());
    mMisMatchPoss = mArith.ln2Poss(mParams.misMatchLn());
    mDelOpen = Math.exp(mParams.deleteOpenLn());
    mDeleteOpenPoss = mArith.ln2Poss(mParams.deleteOpenLn());
    mDeleteExtendPoss = mArith.ln2Poss(mParams.deleteExtendLn());
    mInsertOpenPoss = mArith.ln2Poss(mParams.insertOpenLn());
    mInsertExtendPoss = mArith.ln2Poss(mParams.insertExtendLn());
    mOneMinusDeleteExtendPoss = mArith.prob2Poss(1.0 - Math.exp(mParams.deleteExtendLn()));
    mOneMinusDeleteOpenPoss = mArith.prob2Poss(1.0 - mDelOpen);
    mOneMinusInsertExtendPoss = mArith.prob2Poss(1.0 - Math.exp(mParams.insertExtendLn()));
    mOneMinusDeleteInsertOpenPoss = mArith.prob2Poss(1.0 - mDelOpen - Math.exp(mParams.insertOpenLn()));
    mDeleteOpenInFourPoss = mArith.multiply(mArith.ln2Poss(mParams.deleteOpenLn()), mOneInFourPoss);

    mLength = -1;
    mMaxLength = -1;
    mWidth = -1;
    mMaxWidth = -1;
    mDeleteStartPoss = Double.NaN;
    mMatchStartPoss = Double.NaN;
    mDeleteStartPoss = mArith.multiply(mArith.prob2Poss(mDelOpen), mArith.prob2Poss(1.0));
    mMatchStartPoss = mArith.prob2Poss(1.0 - mDelOpen);
  }

  @Override
  public PossibilityArithmetic arithmetic() {
    return mArith;
  }

  @Override
  public void setEnv(final Environment env) {
    SET_ENV.increment();
    final int width = 2 * env.maxShift() + 1;
    if (env.readLength() > mMaxLength || width > mMaxWidth) {
      resizeMatrix(env.readLength(), width);
    }
    mLength = env.readLength();
    mWidth = width;
    mEnv = env;
    calculateProbabilities();
  }

  void resizeMatrix(int length, int width) {
    SET_ENV_RESIZE.increment();
    mMaxLength = length;
    mMaxWidth = width;
    mMatch = new double[mMaxLength + 1][mMaxWidth];
    mInsert = new double[mMaxLength + 1][mMaxWidth];
    mDelete = new double[mMaxLength + 1][mMaxWidth];
    mEndScores = new double[width];
  }

  /**
   * How far a given row is shifted along the template, relative to
   * the expected start position of the read.  For CG, this accounts for
   * the CG gaps - it is used to keep the middle of the alignment band
   * in line with the most common offsets of the CG gaps.
   *
   * @param row one-based read position.
   * @return an offset along the template, from where the read is expected to start.
   *         Smallest value will be for row 0.
   *         Largest value will be for the last row.
   *         But the values for intermediate rows may move backwards
   *         and forwards between those bounds.
   */
  protected int rowOffset(final int row) {
    return row - mEnv.maxShift() - 1;
  }

  /**
   * Calculate all the probabilities in the matrix and record the
   * total probability of the read.
   */
  protected abstract void calculateProbabilities();

  protected final void calculateInitialRow(final int initRow, final double delete, final double match) {
    for (int j = 0; j < mWidth; j++) {
      mDelete[initRow][j] = delete;
      mMatch[initRow][j] = match;
      mInsert[initRow][j] = mZeroPoss;
    }
  }

  /**
   * Calculate the match/mismatch cost at a given cell of the matrix.
   * @param i zero-based read position
   * @param j zero-based column position (<code>0 .. mWidth - 1</code>)
   * @return natural log of the probability
   */
  protected final double matchEq(final int i, final int j) {
    final byte te = mEnv.template(templateIndex(i, j));
    return matchEqTe(i, te);
  }

  /**
   * @param i read position in internal co-ordinates (0 based).
   * @param j template position in internal co-ordinates (0 based)
   * @return relative template position (suitable for interrogating environment).
   */
  protected int templateIndex(final int i, final int j) {
    return rowOffset(i + 1) + j;
  }

  /**
   * Calculate the match/mismatch probability at a given row and for a specified nucleotide.
   * @param i zero-based read position
   * @param nt nucleotide (assumed to be on template, 0=N...4=T).
   * @return probability represented as a possibility.
   */
  final double matchEqTe(final int i, final byte nt) {
    final byte re = mEnv.read(i);
    //System.err.println("         i=" + i + " j=" + j + " te=" + te + " re=" + re + " sum=" + sum);
    final double incr;
    if (nt == 0 || re == 0) {
      incr = mOneInFourPoss;
    } else {
      final double q = mEnv.quality(i);
      final double q3 = q / 3.0;
      if (nt == re) {
        incr = mArith.add(mArith.multiply(mMatchPoss, mArith.prob2Poss(1.0 - q)),
            mArith.multiply(mMisMatchPoss, mArith.prob2Poss(q3)));
        //System.err.println("match    i=" + i + " j=" + j + " incr=" + incr + " res=" + (sum + incr));
      } else {
        final double x = mArith.multiply(mMatchPoss, mArith.prob2Poss(q3));
        final double y = mArith.multiply(mMisMatchPoss, mArith.prob2Poss((1.0 - q3) / 3.0));
        incr = mArith.add(x, y);
        //System.err.println("mismatch i=" + i + " j=" + j + " x=" + x + " y=" + y + " incr=" + incr + " res=" + (sum + incr));
      }
    }
    return incr;
  }

  @Override
  public abstract double totalScoreLn();

  final int length() {
    return mLength;
  }

  final int width() {
    return mWidth;
  }

  @Override
  public final void toString(final StringBuilder sb) {
    if (mLength == -1) {
      sb.append("ScoreMatrix uninitialised").append(LS);
      return;
    }
    final char[] dnaChars = DNA.valueChars();
    final int rowStart = rowOffset(0);
    final int rowEnd = rowOffset(mLength) + mWidth;
    printTemplateRow(sb, rowStart, rowEnd, mEnv, "ScoreMatrix");
    for (int row = 0; row <= mLength; row++) {
      sb.append(StringUtils.padBetween("[", 5, row + "]"));
      sb.append(row == 0 ? ' ' : dnaChars[mEnv.read(row - 1)]);
      // indent the row, so we line up with the template
      for (int i = 0; i < rowOffset(row) - rowStart; i++) {
        sb.append(StringUtils.padLeft("|", 3 * FIELD_WIDTH + 1));
      }
      for (int j = 0; j < mWidth; j++) {
        sb.append(format(mArith.poss2Ln(mInsert[row][j])));
        sb.append(format(mArith.poss2Ln(mMatch[row][j])));
        sb.append(format(mArith.poss2Ln(mDelete[row][j])));
        sb.append("|");
      }
      sb.append(LS);
    }
    printTemplateRow(sb, rowStart, rowEnd, mEnv, "");
  }

  static String format(final double x) {
    return format(x, FIELD_WIDTH);
  }

  /**
   * padded format
   * @param x value to format
   * @param fw total width
   * @return formated string
   */
  public static String format(final double x, final int fw) {
    //be careful about outputting -0.0
    final String fst = Double.isInfinite(x) && x < 0.0 ? "" : x == 0.0 ? Utils.realFormat(0.0, 3) : Utils.realFormat(-x, 3);
    return StringUtils.padLeft(fst, fw);
  }

  static void printTemplateRow(final StringBuilder sb, final int rowStart, final int rowEnd, final Environment env, final String msg) {
    // print the header row, showing the template.
    final char[] dnaChars = DNA.valueChars();
    sb.append(StringUtils.padBetween(msg, 7 + 3 * FIELD_WIDTH, "|"));
    for (int pos = rowStart + 1; pos <= rowEnd; pos++) {
      sb.append(dnaChars[env.template(pos)]);
      sb.append(StringUtils.padLeft(Integer.toString(env.absoluteTemplatePosition(pos)), 2 * FIELD_WIDTH - 1));
      sb.append(StringUtils.padLeft("|", FIELD_WIDTH + 1));
    }
    sb.append(LS);
  }

  @Override
  public boolean globalIntegrity() {
    integrity();
    for (int i = 0; i < mLength; i++) {
      // the three matrices have exactly the same shape.
      Exam.assertEquals(mMaxWidth, mMatch[i].length);
      Exam.assertEquals(mMaxWidth, mDelete[i].length);
      Exam.assertEquals(mMaxWidth, mInsert[i].length);
      for (int j = 0; j < mWidth; j++) {
        final double vi = mInsert[i][j];
        Exam.assertTrue("i=" + i + " j=" + j + " vi=" + vi, mArith.isValidPoss(vi));
        final double vm = mMatch[i][j];
        Exam.assertTrue("i=" + i + " j=" + j + " vm=" + vm, mArith.isValidPoss(vm));
        final double vd = mDelete[i][j];
        Exam.assertTrue("i=" + i + " j=" + j + " vd=" + vd, mArith.isValidPoss(vd));
      }
    }
    return true;
  }

  @Override
  public boolean integrity() {
    Exam.assertEquals(mArith.zero(), mZeroPoss);
    Exam.assertEquals(mArith.one(), mOnePoss);
    Exam.assertTrue(mArith.isValidPoss(mOneInFourPoss));
    Exam.assertTrue(mArith.isValidPoss(mMatchPoss));
    Exam.assertTrue(mArith.isValidPoss(mMisMatchPoss));
    Exam.assertTrue(mArith.isValidPoss(mDeleteOpenPoss));
    Exam.assertTrue(mArith.isValidPoss(mDeleteExtendPoss));
    Exam.assertTrue(mArith.isValidPoss(mInsertOpenPoss));
    Exam.assertTrue(mArith.isValidPoss(mInsertExtendPoss));
    Exam.assertTrue(mArith.isValidPoss(mOneMinusDeleteExtendPoss));
    Exam.assertTrue(mArith.isValidPoss(mOneMinusDeleteOpenPoss));
    Exam.assertTrue(mArith.isValidPoss(mOneMinusInsertExtendPoss));
    Exam.assertTrue(mArith.isValidPoss(mOneMinusDeleteInsertOpenPoss));
    Exam.assertTrue(mArith.isValidPoss(mDeleteOpenInFourPoss));
    Exam.assertTrue(0.0 <= mDelOpen && mDelOpen <= 1.0 && !Double.isNaN(mDelOpen));

    if (mLength == -1) {
      Exam.assertEquals(-1, mWidth);
      Exam.assertTrue(mEnv == null);
      Exam.assertTrue(mMatch == null);
      Exam.assertTrue(mDelete == null);
      Exam.assertTrue(mInsert == null);
      Exam.assertTrue(Double.isNaN(mDeleteStartPoss));
      Exam.assertTrue(Double.isNaN(mMatchStartPoss));
    } else {
      Exam.assertTrue(mArith.isValidPoss(mDeleteStartPoss));
      Exam.assertTrue(mArith.isValidPoss(mMatchStartPoss));
      Exam.assertTrue(0 < mLength);
      Exam.assertTrue(0 < mWidth);
      Exam.assertTrue(mWidth <= mMaxWidth);
      Exam.assertTrue(mLength <= mMaxLength);
      Exam.assertTrue(rowOffset(0) < rowOffset(mLength));
      Exam.assertEquals(mMaxLength + 1, mMatch.length);
      Exam.assertEquals(mMaxLength + 1, mDelete.length);
      Exam.assertEquals(mMaxLength + 1, mInsert.length);
      Exam.assertNotNull(mEnv);
    }
    return true;
  }

  double insert(final int row, final int col) {
    return mInsert[row][col];
  }

  protected final void setInsert(final int row, final int col, final double poss) {
    assert !Double.isNaN(poss) && poss != Double.POSITIVE_INFINITY : poss + " @ " + row + ":" + col;
    mInsert[row][col] = poss;
  }

  double delete(final int row, final int col) {
    return mDelete[row][col];
  }

  protected final void setDelete(final int row, final int col, final double poss) {
    assert !Double.isNaN(poss) && poss != Double.POSITIVE_INFINITY : poss + " @ " + row + ":" + col;
    mDelete[row][col] = poss;
  }

  double match(final int row, final int col) {
    return mMatch[row][col];
  }

  protected final void setMatch(final int row, final int col, final double poss) {
    assert !Double.isNaN(poss) && poss != Double.POSITIVE_INFINITY : poss + " @ " + row + ":" + col;
    mMatch[row][col] = poss;
  }
}
