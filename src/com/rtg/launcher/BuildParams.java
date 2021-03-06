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
package com.rtg.launcher;

import java.io.File;
import java.io.IOException;

import com.rtg.index.params.CreateParams;
import com.rtg.mode.SequenceMode;
import com.rtg.reader.SequencesReader;
import com.rtg.util.Params;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.integrity.Exam;

/**
 * Holds a set of parameters that are sufficient to enable the initial creation
 * and build of an index.
 *
 */
public final class BuildParams extends CreateParams implements Params {

  static long size(final int windowSize, final int stepSize, final ISequenceParams sequenceParams) throws IOException {
    final SequenceMode mode = sequenceParams.mode();
    final HashingRegion region = sequenceParams.region(); // XXX This should probably be sequenceParams.readerRestriction()??
    final long st;
    final long en;
    if (region != HashingRegion.NONE) {
      st = region.getStart();
      en = region.getEnd();
    } else {
      st = 0;
      en = sequenceParams.reader().numberSequences();
    }
    final SequencesReader reader = sequenceParams.reader();
    final long length = reader.lengthBetween(st, en);
    //Diagnostic.developerLog("start=" + st + " end=" + en + " size=" + length + " windowSize=" + windowSize + " stepSize=" + stepSize);
    return size(length, en - st, windowSize, stepSize, mode.numberFrames(), mode.codeIncrement());
  }

  /**
   * Compute the total number of hash windows given a sequence length and other values.
   * @param length of sequence.
   * @param numSequences number of sequences
   * @param windowSize window size.
   * @param stepSize step size.
   * @param frames number of frames.
   * @param codeIncrement number of residues in one code (needed for translation).
   * @return the total number of hash windows.
   */
  public static long size(final long length, long numSequences, final int windowSize, final int stepSize, final int frames, final int codeIncrement) {
    final long l = (length / codeIncrement) - windowSize;
    if (l < 0) {
      return 0;
    }
    long size = ((l / stepSize) + 1) * frames;
    if (stepSize > windowSize) { // When stepsize is larger than windowsize the above formula may underestimate the number of hashes per sequence by at most one hash per sequence
      size += numSequences;
    }
    Diagnostic.developerLog("size=" + size + " length=" + length + " l=" + l + " windowSize=" + windowSize + " stepSize=" + stepSize + " codeIncrement=" + codeIncrement + " frames=" + frames);
    //System.err.println("size=" + size + " length=" + length + " l=" + l + " windowSize=" + windowSize + " stepSize=" + stepSize + " codeIncrement=" + codeIncrement);
    return size;
  }

  /**
   * Calculate the number of hash bits needed.
   * @param typeBits bits needed for each residue.
   * @param windowSize number of residues in a hash window.
   * @return the number of required hash bits.
   */
  //implemented as a static so that can be called in the super constructor
  static int calculateHashBits(final int typeBits, final int windowSize) {
    final int winBits = windowSize * typeBits;
    return winBits > 64 ? 64 : winBits;
  }


  /**
   * Creates a BuildParams builder.
   * @return the builder.
   */
  public static BuildParamsBuilder builder() {
    return new BuildParamsBuilder();
  }


  /**
   * A builder class for <code>BuildParams</code>.
   */
  public static class BuildParamsBuilder {

    protected ISequenceParams mSequenceParams;

    protected int mIdBits = 31;

    protected int mWindowSize;

    protected int mStepSize;

    protected boolean mCompressHashes;

    protected boolean mCreateBitVector = true;

    protected boolean mDefaultSize = true;
    protected long mSize;  // May be calculated automatically

    protected int mTypeBits; // Calculated automatically
    protected int mHashBits; // Calculated automatically

    protected boolean mSpaceEfficientBufUnsafe = false;

    protected boolean mIdeal = false;

    /**
     * Sets the sequences that will be used to build this index.
     * @param sequences the sequence parameters.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder sequences(final ISequenceParams sequences) {
      mSequenceParams = sequences;
      return this;
    }

    /**
     * Sets index size. If not set, a default will be determined from the other parameters.
     * @param size upper bound of number of hash windows expected in index.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder size(final long size) {
      mSize = size;
      mDefaultSize = false;
      return this;
    }

    /**
     * Sets the number of bits needed to represent an identifier value.
     * @param idBits number of bits needed to represent an identifier value.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder idBits(final int idBits) {
      mIdBits = idBits;
      return this;
    }

    /**
     * Sets window size.
     * @param windowSize number of residues in a hash window.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder windowSize(final int windowSize) {
      mWindowSize = windowSize;
      return this;
    }

    /**
     * Sets number of residues between the start of succesive windows.
     * @param stepSize number of residues between the start of succesive windows.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder stepSize(final int stepSize) {
      mStepSize = stepSize;
      return this;
    }

    /**
     * Sets whether to compress the hash array.
     * @param compressHashes if we are compressing the hash array (requires two passes of adds).
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder compressHashes(final boolean compressHashes) {
      mCompressHashes = compressHashes;
      return this;
    }

    /**
     * Sets whether to create the Bit Vector.
     * @param createBitVector if we are creating the bit vector
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder createBitVector(final boolean createBitVector) {
      mCreateBitVector = createBitVector;
      return this;
    }

    /**
     * Sets whether to use the <code>spaceEfficientButUnsafe</code> selection of <code>ArrayType</code>
     * @param fanatical if we want to use the <code>spaceEfficientButUnsafe</code> <code>ArrayType</code> selection mode
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder spaceEfficientButUnsafe(final boolean fanatical) {
      mSpaceEfficientBufUnsafe = fanatical;
      return this;
    }

    /**
     * Sets whether to minimise the memory used.
     * Only use if not intending to search the index.
     * @param ideal true iff to minimise memory.
     * @return this builder, so calls can be chained.
     */
    public BuildParamsBuilder ideal(final boolean ideal) {
      mIdeal = ideal;
      return this;
    }

    /**
     * Creates a BuildParams using the current builder
     * configuration.
     * @return the new BuildParams
     * @throws IOException If in I/O error occurs while finding out sizes of data
     */
    public BuildParams create() throws IOException {
      if (mDefaultSize) {
        mSize = BuildParams.size(mWindowSize, mStepSize, mSequenceParams);
      }
      if (mSequenceParams == null) {
        throw new NullPointerException("BuildParams requires SequenceParams to be set.");
      }
      mTypeBits = mSequenceParams.mode().codeType().bits();
      mHashBits = BuildParams.calculateHashBits(mTypeBits, mWindowSize);
      return new BuildParams(this);
    }

  }



  private final ISequenceParams mSequenceParams;

  /** Number of bits needed to represent a single residue (3 or 5). */
  private final int mTypeBits;

  private final int mWindowSize;

  private final int mStepSize;

  /**
   * Create a set of parameters to use in building a SLIM index.
   *
   * @param builder the builder object.
   */
  public BuildParams(final BuildParamsBuilder builder) {
    super(builder.mSize,
          builder.mHashBits,
          builder.mTypeBits * builder.mWindowSize,
          builder.mIdBits, builder.mCompressHashes,
          builder.mCreateBitVector, builder.mSpaceEfficientBufUnsafe,
          builder.mIdeal
          );
    mSequenceParams = builder.mSequenceParams;
    mTypeBits = builder.mTypeBits;
    mWindowSize = builder.mWindowSize;
    mStepSize = builder.mStepSize;
    integrity();
  }

  /**
   * Create a builder with all the values set to those of this object.
   * @return a builder
   */
  public BuildParamsBuilder cloneBuilder() {
    return new BuildParamsBuilder().windowSize(windowSize()).stepSize(stepSize()).sequences(sequences())
      .compressHashes(compressHashes());
  }

  /**
   * Create a new parameters object where everything is the same except for the range in the subsequence.
   * @param region of subsequence to construct sub params for
   * @return the new <code>BuildParams</code>.
   * @throws IOException If in I/O error occurs
   */
  public BuildParams subSequence(final HashingRegion region) throws IOException {
    //System.err.println(sequences.getClass().getName() + "=sequences class " + sequences + "=sequences");
    return cloneBuilder().sequences(sequences().subSequence(region)).create();
  }

  /**
   * Get the directory where the subject sequences are to be found.
   * @return the directory where the subject sequences are to be found.
   */
  public File directory() {
    return mSequenceParams.directory();
  }

  /**
   * Get the sequence parameters.
   * @return the sequence parameters.
   */
  public ISequenceParams sequences() {
    return mSequenceParams;
  }

  /**
   * Get the number of residues in a hash window.
   * @return the number of residues in a hash window.
   */
  public int windowSize() {
    return mWindowSize;
  }

  /**
   * Get the number of residues between the start of succesive windows.
   * @return the number of residues between the start of succesive windows.
   */
  public int stepSize() {
    return mStepSize;
  }

  @Override
  public void close() throws IOException {
    mSequenceParams.close();
  }

  /**
   * Check if queries are closed.
   * @return true iff the queries are currently closed.
   */
  @Override
  public boolean closed() {
    return mSequenceParams.closed();
  }

  @Override
  public int hashCode() {
    return Utils.pairHash(super.hashCode(), Utils.pairHash(Utils.pairHash(mSequenceParams.hashCode(), mTypeBits), Utils.pairHash(mWindowSize, mStepSize)));
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    final BuildParams that = (BuildParams) obj;
    return
    super.equals(that)
    && this.mSequenceParams.equals(that.mSequenceParams)
    && this.mTypeBits == that.mTypeBits
    && this.mWindowSize == that.mWindowSize
    && this.mStepSize == that.mStepSize;
  }


  @Override
  public boolean integrity() {
    super.integrity();
    Exam.assertTrue(mSequenceParams != null);
    Exam.assertTrue("mTypeBits=" + mTypeBits, mTypeBits == 2 || mTypeBits == 5);
    Exam.assertTrue(mWindowSize >= 1);
    Exam.assertTrue("stepSize=" + mStepSize + " windowSize=" + mWindowSize, mStepSize >= 1);
    return true;
  }


  @Override
  public boolean globalIntegrity() {
    integrity();
    Exam.integrity(mSequenceParams);
    return true;
  }

  @Override
  public String toString() {
    return " seq={" + mSequenceParams + "} "
     + super.toString()
     + " window=" + mWindowSize
     + " step=" + mStepSize;
  }
}
