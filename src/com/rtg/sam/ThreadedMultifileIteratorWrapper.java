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

package com.rtg.sam;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.rtg.util.PopulatorFactory;
import com.rtg.util.intervals.RangeList;

import net.sf.samtools.SAMFileHeader;

/**
 * Used to prevent having to create a separate ThreadedMultifileIterator for each reference
 * sequence being processed.
 */
public class ThreadedMultifileIteratorWrapper<T extends ReaderRecord<T> & MateInfo> implements RecordIterator<T> {

  private final RecordIterator<T> mIterator;
  private final SamReadingContext mContext;
  private T mNext;
  private int mSequenceId = -1;
  private RangeList<String> mCurrentRangeList;

  /**
   * Convenience constructor
   * @param files SAM/BAM files
   * @param numThreads the number of threads to use (maximum)
   * @param populatorFactory populator factory for records
   * @param filterParams parameters for filtering
   * @param headerOverride use this header instead of one present in file. (null to use one in file)
   * @throws IOException if an IO error occurs
   */
  public ThreadedMultifileIteratorWrapper(Collection<File> files, int numThreads, PopulatorFactory<T> populatorFactory, SamFilterParams filterParams, SAMFileHeader headerOverride) throws IOException {
    this(new SamReadingContext(files, numThreads, filterParams, headerOverride), populatorFactory);
  }

  /**
   * Constructor
   * @param context the SAM reading context
   * @param populatorFactory populator factory for records
   * @throws IOException if an IO error occurs
   */
  public ThreadedMultifileIteratorWrapper(SamReadingContext context, PopulatorFactory<T> populatorFactory) throws IOException {
    final ThreadedMultifileIterator<T> it = new ThreadedMultifileIterator<>(context, populatorFactory);
    mContext = context;
    mIterator = context.filterParams().findAndRemoveDuplicates() ? new DedupifyingRecordIterator<>(it) : it;
    if (mIterator.hasNext()) {
      mNext = mIterator.next();
    } else {
      mNext = null;
    }
  }


  /**
   * Set the current reference sequence index
   * @param index reference sequence index
   */
  public void setSequenceId(int index) {
    assert index > mSequenceId : index + " > " + mSequenceId; //incoming index should always be greater than previous index
    mSequenceId = index;
    while (mNext != null && index != mNext.getSequenceId() && index > mNext.getSequenceId()) { //update mNext until we get to next sequence
      if (mIterator.hasNext()) {
        mNext = mIterator.next();
      } else {
        mNext = null;
      }
    }
    mCurrentRangeList = mContext.hasRegions() ? mContext.rangeList(mSequenceId) : null;
  }

  @Override
  public boolean hasNext() {
    if (mSequenceId < 0) {
      throw new IllegalStateException("reference is not set");
    }
    return mNext != null && mNext.getSequenceId() == mSequenceId;
  }

  @Override
  public T next() {
    final T res = mNext;
    if (mIterator.hasNext()) {
      mNext = mIterator.next();
    } else {
      mNext = null;
    }
    return res;
  }

  @Override
  public void remove() {
  }

  @Override
  public void close() throws IOException {
    mIterator.close();
  }

  @Override
  public SAMFileHeader header() {
    return mIterator.header();
  }

  @Override
  public long getTotalNucleotides() {
    return mIterator.getTotalNucleotides();
  }

  @Override
  public long getInvalidRecordsCount() {
    return mIterator.getInvalidRecordsCount();
  }

  @Override
  public long getFilteredRecordsCount() {
    return mIterator.getFilteredRecordsCount();
  }

  @Override
  public long getDuplicateRecordsCount() {
    return mIterator.getDuplicateRecordsCount();
  }

  @Override
  public long getOutputRecordsCount() {
    return mIterator.getOutputRecordsCount();
  }

  @Override
  public long getTotalRecordsCount() {
    return mIterator.getTotalRecordsCount();
  }

  /**
   * Get the context used for input reading
   * @return the context
   */
  public SamReadingContext context() {
    return mContext;
  }

  /**
   * Get the ranges applying to the current region
   * @return the ranges.
   */
  public RangeList<String> getCurrentRangeList() {
    return mCurrentRangeList;
  }

}
