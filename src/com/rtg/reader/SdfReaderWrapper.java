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
package com.rtg.reader;

import java.io.File;
import java.io.IOException;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.mode.SequenceType;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Wrapper for SDF readers that can handle single end or paired end reads
 *
 */
@TestClass(value = "com.rtg.reader.SdfSplitterTest")
public final class SdfReaderWrapper {

  private final boolean mIsPaired;
  private final SequencesReader mSingle;
  private final SequencesReader mLeft;
  private final SequencesReader mRight;

  static SequencesReader createSequencesReader(File sdf, boolean useMem) throws IOException {
    if (useMem) {
      return SequencesReaderFactory.createMemorySequencesReaderCheckEmpty(sdf, true, true, LongRange.NONE);
    } else {
      return SequencesReaderFactory.createDefaultSequencesReaderCheckEmpty(sdf);
    }
  }

  /**
   * Wrapper for the readers.
   * @param sdfDir the directory containing the sequences.
   * @param useMem to use memory sequences reader or not.
   * @param checkConsistency to check paired end consistency or not.
   * @throws IOException whenever
   */
  public SdfReaderWrapper(File sdfDir, boolean useMem, boolean checkConsistency) throws IOException {
    mIsPaired = ReaderUtils.isPairedEndDirectory(sdfDir);
    if (mIsPaired) {
      mSingle = null;
      mLeft = createSequencesReader(ReaderUtils.getLeftEnd(sdfDir), useMem);
      mRight = createSequencesReader(ReaderUtils.getRightEnd(sdfDir), useMem);
      if (checkConsistency) {
        if (mLeft.numberSequences() != mRight.numberSequences()
            || !mLeft.type().equals(mRight.type())
            || mLeft.hasQualityData() != mRight.hasQualityData()
            || mLeft.hasNames() != mRight.hasNames()) {
          throw new NoTalkbackSlimException(ErrorType.INFO_ERROR, "Paired end SDF has inconsistencies between arms.");
        }
      }
    } else {
      mLeft = null;
      mRight = null;
      mSingle = createSequencesReader(sdfDir, useMem);
    }
  }

  /**
   * Convenience constructor to create a wrapper for existing readers.
   * @param left the left or single reader
   * @param right the right reader or null if using single
   */
  public SdfReaderWrapper(SequencesReader left, SequencesReader right) {
    assert left != null;
    mIsPaired = right != null;
    if (mIsPaired) {
      mSingle = null;
      mLeft = left;
      mRight = right;
    } else {
      mLeft = null;
      mRight = null;
      mSingle = left;
    }
  }

  /**
   * If the contained readers are pair.
   * @return true if the contained readers are paired, false otherwise
   */
  public boolean isPaired() {
    return mIsPaired;
  }

  /**
   * The left hand reader of the pair.
   * @return The left hand reader of the pair
   */
  public SequencesReader left() {
    assert mIsPaired;
    return mLeft;
  }

  /**
   * The right hand reader of the pair.
   * @return The right hand reader of the pair
   */
  public SequencesReader right() {
    assert mIsPaired;
    return mRight;
  }

  /**
   * The single reader.
   * @return The single reader
   */
  public SequencesReader single() {
    assert !mIsPaired;
    return mSingle;
  }

  /**
   * Convenience method for seeking to a particular sequence id
   * @param seqId the sequence id to seek to
   * @throws IOException when one of the sub-readers IOException.
   */
  public void seek(long seqId) throws IOException {
    if (mIsPaired) {
      mLeft.seek(seqId);
      mRight.seek(seqId);
    } else {
      mSingle.seek(seqId);
    }
  }

  /**
   * Convenience method for moving the readers on to the next sequence.
   * @return true if there is another sequence to be read, false otherwise.
   * @throws IOException when one of the sub-readers IOException.
   */
  public boolean nextSequence() throws IOException {
    if (mIsPaired) {
      return mLeft.nextSequence() && mRight.nextSequence();
    } else {
      return mSingle.nextSequence();
    }
  }

  /**
   * Convenience method for getting the maximum sequence length
   * @return the maximum sequence length
   */
  public int maxLength() {
    if (mIsPaired) {
      return (int) Math.max(mLeft.maxLength(), mRight.maxLength());
    } else {
      return (int) mSingle.maxLength();
    }
  }

  /**
   * Convenience method for getting if the contained readers have quality data.
   * @return true if contained readers have quality data, false otherwise.
   */
  public boolean hasQualityData() {
    if (mIsPaired) {
      return mLeft.hasQualityData();
    } else {
      return mSingle.hasQualityData();
    }
  }

  /**
   * Convenience method for getting if the contained readers have name data.
   * @return true if contained readers have name data, false otherwise.
   */
  public boolean hasNames() {
    if (mIsPaired) {
      return mLeft.hasNames() && mRight.hasNames();
    } else {
      return mSingle.hasNames();
    }
  }

  /**
   * Convenience method.
   * @return PrereadType.
   */
  public PrereadType getPrereadType() {
    if (mIsPaired) {
      return mLeft.getPrereadType();
    } else {
      return mSingle.getPrereadType();
    }
  }

  /**
   * Convenience method.
   * @return SequenceType
   */
  public SequenceType type() {
    if (mIsPaired) {
      return mLeft.type();
    } else {
      return mSingle.type();
    }
  }

  /**
   * Convenience method.
   * @return current name.
   * @throws IOException whenever
   * @throws IllegalStateException whenever
   */
  public String currentName() throws IllegalStateException, IOException {
    if (hasNames()) {
      if (mIsPaired) {
        return mLeft.currentName();
      } else {
        return mSingle.currentName();
      }
    }
    return null;
  }

  /**
   * Convenience method
   * @return current full name
   * @throws IllegalStateException whenever
   * @throws IOException whenever
   */
  public String currentFullName() throws IllegalStateException, IOException {
    if (hasNames()) {
      if (mIsPaired) {
        return mLeft.currentFullName();
      } else {
        return mSingle.currentFullName();
      }
    }
    return null;
  }

  /**
   * Convenience method.
   * @return current right arm name.
   * @throws IOException whenever
   * @throws IllegalStateException whenever
   */
  public String currentRightFullName() throws IllegalStateException, IOException {
    if (mIsPaired) {
      if (hasNames()) {
        return mRight.currentFullName();
      }
      return null;
    } else {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Convenience method.
   * @return current sequence id.
   * @throws IllegalStateException whenever
   */
  public long currentSequenceId() {
    if (mIsPaired) {
      return mLeft.currentSequenceId();
    } else {
      return mSingle.currentSequenceId();
    }
  }

  /**
   * Convenience method.
   * @return number of sequences.
   */
  public long numberSequences() {
    if (mIsPaired) {
      return mLeft.numberSequences();
    } else {
      return mSingle.numberSequences();
    }
  }

  /**
   * Closes the internal Sequence Readers
   * @throws IOException when one of the Sequence Readers close methods fails
   */
  public void close() throws IOException {
    try {
      if (mSingle != null) {
        mSingle.getPrereadType();
        mSingle.close();
      }
    } finally {
      try {
        if (mLeft != null) {
          mLeft.close();
        }
      } finally {
        if (mRight != null) {
          mRight.close();
        }
      }
    }
  }
}
