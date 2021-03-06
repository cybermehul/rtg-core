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

import java.util.Arrays;

import com.rtg.reader.FastqSequenceDataSource;
import com.rtg.sam.MateInfo;
import com.rtg.sam.ReaderRecord;
import com.rtg.sam.SamUtils;
import com.rtg.util.intervals.SequenceIdLocusSimple;

import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord;

/**
 * Alignment information needed by variant implementations.
 */
public class VariantAlignmentRecord extends SequenceIdLocusSimple implements ReaderRecord<VariantAlignmentRecord>, MateInfo, MapInfo {

  /**
   * Record used to denote an overflow condition, not a true record.
   * @param start 0-based start position of overflow
   * @param length of overflow
   * @return overflow record
   */
  public static VariantAlignmentRecord overflow(final int start, final int length) {
    return new VariantAlignmentRecord(start, start + length);
  }

  private final byte[] mBases;
  private final byte[] mQuality;
  private final String mCigar;
  private final byte mMappingQuality;
  private final byte mFlag;
  private final int mAmbiguity;
  private final int mAlignmentScore;
  private final int mMateSequenceId;
  private final int mFragmentLength;

  private final SAMReadGroupRecord mReadGroup;
  private final String mSuperCigar;
  private final String mOverlapBases;
  private final String mOverlapQuality;
  private final String mOverlapInstructions;
  private final String mCgReadDelta;

  private final int mGenome;

  private VariantAlignmentRecord(final int start, final int end) {
    super(0, start, end);
    mBases = null;
    mQuality = null;
    mCigar = null;
    mMappingQuality = 0;
    mFlag = -1;
    mAmbiguity = 0;
    mAlignmentScore = 0;
    mMateSequenceId = 0;
    mFragmentLength = 0;
    mReadGroup = null;
    mSuperCigar = null;
    mOverlapBases = null;
    mOverlapQuality = null;
    mOverlapInstructions = null;
    mCgReadDelta = null;
    mGenome = 0;
  }

  /**
   * Construct a new alignment record populated from a SAM record.
   * @param record SAM record. Requires header with sequence dictionary (for reference index lookup)
   * @param genome genome code for this record
   */
  public VariantAlignmentRecord(final SAMRecord record, final int genome) {
    super(record.getReferenceIndex(), record.getAlignmentStart() - 1, record.getReadUnmappedFlag() ? record.getAlignmentStart() - 1 + record.getReadLength() : record.getAlignmentEnd()); // picard end position is 1-based inclusive == 0-based exclusive
    mGenome = genome;
    mFragmentLength = record.getInferredInsertSize();
    mBases = record.getReadBases();
    final byte[] baseQualities = record.getBaseQualities();
    if (baseQualities.length == 0) {
      mQuality = baseQualities;
    } else {
      mQuality = Arrays.copyOf(baseQualities, baseQualities.length);
      for (int i = 0; i < mQuality.length; i++) {
        mQuality[i] += FastqSequenceDataSource.PHRED_LOWER_LIMIT_CHAR;
      }
    }
    mCigar = record.getCigarString();
    mMappingQuality = (byte) record.getMappingQuality();
    mReadGroup = record.getReadGroup();
    final Integer v = SamUtils.getNHOrIH(record);
    mAmbiguity = v == null ? -1 : v;
    final Integer as = record.getIntegerAttribute("AS");
    mAlignmentScore = as == null ? -1 : as;
    mSuperCigar = record.getStringAttribute(SamUtils.CG_SUPER_CIGAR);
    mMateSequenceId = record.getMateReferenceIndex();
    byte f = 0;
    if (record.getReadPairedFlag() && record.getProperPairFlag()) {
      f += 1;
    }
    if (record.getReadPairedFlag()) {
      f += 2;
      if (record.getFirstOfPairFlag() ^ record.getReadNegativeStrandFlag()) { //CG stupidity
        f += 4;
      }
    }
    if (record.getReadNegativeStrandFlag()) {
      f += 8;
    }
    if (record.getReadUnmappedFlag()) {
      f += 16;
    }
    mFlag = f;

    mOverlapQuality = mSuperCigar == null
      ? SamUtils.allowEmpty(record.getStringAttribute(SamUtils.ATTRIBUTE_CG_OVERLAP_QUALITY))
      : SamUtils.allowEmpty(record.getStringAttribute(SamUtils.CG_OVERLAP_QUALITY));
    mOverlapBases = SamUtils.allowEmpty(record.getStringAttribute(SamUtils.ATTRIBUTE_CG_OVERLAP_BASES));
    mOverlapInstructions = record.getStringAttribute(SamUtils.ATTRIBUTE_CG_RAW_READ_INSTRUCTIONS);
    mCgReadDelta = record.getStringAttribute(SamUtils.CG_READ_DELTA);
  }

  /**
   * Test if this record represents an overflow condition.
   * @return if this is an overflow record
   */
  public boolean isOverflow() {
    return mFlag == -1;
  }

  /**
   * Construct a new alignment record populated from a SAM record.
   * @param record SAM record
   */
  public VariantAlignmentRecord(final SAMRecord record) {
    this(record, record.getReferenceIndex());
  }

  public byte[] getRead() {
    return mBases;
  }

  /**
   * Get the ASCII phred quality values as a byte array.
   * @return quality
   */
  public byte[] getQuality() {
    return mQuality;
  }

  public String getCigar() {
    return mCigar;
  }

  @Override
  public int getMappingQuality() {
    return mMappingQuality & 0xFF;
  }

  // todo somehow make this an int
  public SAMReadGroupRecord getReadGroup() {
    return mReadGroup;
  }

  @Override
  public int getNHOrIH() {
    return mAmbiguity;
  }

  @Override
  public boolean isMated() {
    return (mFlag & 1) != 0;
  }

  public boolean isReadPaired() {
    return (mFlag & 2) != 0;
  }

  public boolean isCgOverlapLeft() {
    return (mFlag & 4) != 0;
  }

  public boolean isNegativeStrand() {
    return (mFlag & 8) != 0;
  }

  public boolean isUnmapped() {
    return (mFlag & 16) != 0;
  }

  // todo hopefully can get rid of this ... by rolling into normal cigar field
  public String getSuperCigar() {
    return mSuperCigar;
  }

  public String getOverlapBases() {
    return mOverlapBases;
  }

  public String getOverlapQuality() {
    return mOverlapQuality;
  }

  public String getOverlapInstructions() {
    return mOverlapInstructions;
  }

  public String getCGReadDelta() {
    return mCgReadDelta;
  }

  @Override
  public String toString() {
    return getStart() + " " + getCigar() + " " + new String(getRead()) + " " + new String(getQuality());
  }

  @Override
  public int disambiguateDuplicate(VariantAlignmentRecord rec) {
    final String thisStr = getStart() + " " + getCigar() + " " + new String(getRead()) + " " + new String(getQuality());
    final String thatStr = rec.getStart() + " " + rec.getCigar() + " " + new String(rec.getRead()) + " " + new String(rec.getQuality());
    return thisStr.compareTo(thatStr);
  }

  private static int compare(final byte[] a, final byte[] b) {
    if (a.length != b.length) {
      return a.length - b.length;
    }
    for (int k = 0; k < a.length; k++) {
      if (a[k] != b[k]) {
        return a[k] - b[k];
      }
    }
    return 0;
  }

  private static int compare(final String a, final String b) {
    if (a != null) {
      return a.compareTo(b);
    } else if (b != null) {
      return -1;
    }
    return 0;
  }

  /**
   * Compares the object just on its values. the regular <code>compareTo</code> method will only return 0 for the same instance
   * @param var the object to compare
   * @return <code>-ve</code> for this object is less than, <code>0</code> for equal
   */
  public int valueCompareTo(VariantAlignmentRecord var) {
    // call starting at leftmost sequence, leftmost position, leftmost end
    if (var == this) {
      return 0;
    }
    final int thisRef = getSequenceId();
    final int thatRef = var.getSequenceId();
    if (thisRef == -1 && thatRef != -1) {
      return 1;
    } else if (thatRef == -1 && thisRef != -1) {
      return -1;
    }
    if (thisRef < thatRef) {
      return -1;
    }
    if (thisRef > thatRef) {
      return 1;
    }

    if (var.getStart() > getStart()) {
      return -1;
    } else if (var.getStart() < getStart()) {
      return 1;
    }

    final int aLen = getLength();
    final int bLen = var.getLength();
    if (aLen > bLen) {
      return 1;
    } else if (aLen < bLen) {
      return -1;
    }

    final int c = var.getCigar().compareTo(getCigar());
    if (c != 0) {
      return c;
    }
    final int mq = var.getMappingQuality() - getMappingQuality();
    if (mq != 0) {
      return mq;
    }
    final int r = compare(var.getRead(), getRead());
    if (r != 0) {
      return r;
    }
    final int q = compare(var.getQuality(), getQuality());
    if (q != 0) {
      return q;
    }
    final int f = var.mFlag - mFlag;
    if (f != 0) {
      return f;
    }
    final int as = var.mAlignmentScore - mAlignmentScore;
    if (as != 0) {
      return as;
    }
    final int nh = var.mAmbiguity - mAmbiguity;
    if (nh != 0) {
      return nh;
    }
    final int gg = var.mGenome - mGenome;
    if (gg != 0) {
      return gg;
    }
    final int sc = compare(var.mSuperCigar, mSuperCigar);
    if (sc != 0) {
      return sc;
    }
    return compare(var.mOverlapBases + var.mOverlapQuality + var.mOverlapInstructions + var.mCgReadDelta, mOverlapBases + mOverlapQuality + mOverlapInstructions + mCgReadDelta);
  }

  @Override
  public int compareTo(final VariantAlignmentRecord var) {
    final int ov = valueCompareTo(var);
    if (ov != 0) {
      return ov;
    }
    //    System.out.println(this + " cf. " + var);
    return System.identityHashCode(this) - System.identityHashCode(var);
  }

  @Override
  public boolean equals(final Object var) {
    return this == var;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Genome this record corresponds to.
   * @return genome
   */
  @Override
  public int getGenome() {
    if (mGenome == -1) {
      throw new UnsupportedOperationException();
    }
    return mGenome;
  }

  private VariantAlignmentRecord mChainedRecord = null;

  @Override
  public void setNextInChain(final VariantAlignmentRecord rec) {
    mChainedRecord = rec;
  }

  @Override
  public VariantAlignmentRecord chain() {
    return mChainedRecord;
  }

  @Override
  public int getMateSequenceId() {
    return mMateSequenceId;
  }

  @Override
  public int getFragmentLength() {
    return mFragmentLength;
  }
}

