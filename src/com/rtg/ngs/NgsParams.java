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
package com.rtg.ngs;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import com.rtg.alignment.AlignerMode;
import com.rtg.launcher.BuildParams;
import com.rtg.launcher.ISequenceParams;
import com.rtg.launcher.ModuleParams;
import com.rtg.mode.ProgramMode;
import com.rtg.mode.ProteinScoringMatrix;
import com.rtg.position.output.OutputFormatType;
import com.rtg.position.output.PositionDistributionParams;
import com.rtg.position.output.PositionOutputParams;
import com.rtg.position.output.PositionParams;
import com.rtg.reference.Sex;
import com.rtg.util.MaxShiftFactor;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.ListenerType;
import com.rtg.util.integrity.Exam;
import com.rtg.util.integrity.Integrity;
import com.rtg.util.io.GzipAsynchOutputStream;
import com.rtg.util.machine.MachineOrientation;
import com.rtg.util.test.params.ParamsNoField;

/**
 * Holds all the parameters needed for doing a build and a search.
 */
public class NgsParams extends ModuleParams implements Integrity {

  private final ISequenceParams mBuildFirstParams;
  private final ISequenceParams mBuildSecondParams;
  private final ISequenceParams mSearchParams;
  //  private final Integer mInsertSize;
  private final Integer mMaxFragmentLength;
  private final Integer mMinFragmentLength;
  private final Integer mHashCountThreshold;
  private final Integer mMaxHashCountThreshold;
  private final Integer mMinHashCountThreshold;
  private final boolean mUseProportionalHashThreshold;
  private final int mReadFreqThreshold;
  private final NgsOutputParams mOutputParams;
  private final NgsMaskParams mMaskParams;
  private final Collection<ListenerType> mListeners;
  private final int mNumberThreads;
  private final int mThreadMultiplier;
  private final int mStepSize;
  private final boolean mUseLongReadMapping;
  private final ProteinScoringMatrix mProteinScoringMatrix;
  private final boolean mEnableProteinReadCache;
  private final boolean mCompressHashes;
  private final int mIntSetWindow;
  private final Integer mMinHits;
  private final boolean mLegacyCigars;
  private final boolean mUseTopRandom;
  private final long mMapXMinReadLength;
  private final int mMapXMetaChunkSize;
  private final int mMapXMetaChunkOverlap;
  private final MachineOrientation mPairOrientation;
  private final boolean mParallelUnmatedProcessing;
  private final int mGapOpenPenalty;
  private final int mGapExtendPenalty;
  private final int mSubstitutionPenalty;
  private final int mUnknownsPenalty;
  private final int mSoftClipDistance;
  private final MaxShiftFactor mAlignerBandWidthFactor;
  private final AlignerMode mAlignerMode;
  private final String mSingleIndelPenalties;

  /**
   * Creates a NgsParams builder.
   * @return the builder.
   */
  public static NgsParamsBuilder builder() {
    return new NgsParamsBuilder();
  }

  /**
   * @param builder the builder object.
   */
  protected NgsParams(final NgsParamsBuilder builder) {
    super(builder);
    mUseLongReadMapping = builder.mUseLongReadMapping;
    mNumberThreads = builder.mNumberThreads;
    mThreadMultiplier = builder.mThreadMultiplier;
    //    mInsertSize = builder.mExpectedInsertSize;
    mMaxFragmentLength = builder.mMaxFragmentLength;
    mMinFragmentLength = builder.mMinFragmentLength;
    mStepSize = builder.mStepSize;
    mHashCountThreshold = builder.mHashCountThreshold;
    mMaxHashCountThreshold = builder.mMaxHashCountThreshold;
    mMinHashCountThreshold = builder.mMinHashCountThreshold;
    mBuildFirstParams = builder.mBuildFirstParams;
    mBuildSecondParams = builder.mBuildSecondParams;
    mSearchParams = builder.mSearchParams;
    mOutputParams = builder.mOutputParams;
    mMaskParams = builder.mMaskParams;
    mListeners = builder.mListeners;
    mProteinScoringMatrix = builder.mProteinScoringMatrix;
    mEnableProteinReadCache = builder.mEnableProteinReadCache;
    mCompressHashes = builder.mCompressHashes;
    mIntSetWindow = builder.mIntSetWindow;
    mReadFreqThreshold = builder.mReadFreqThreshold;
    mMinHits = builder.mMinHits;
    mUseProportionalHashThreshold = builder.mUseProportionalHashThreshold;
    mLegacyCigars = builder.mLegacyCigars;
    mUseTopRandom = builder.mUseTopRandom;
    mMapXMinReadLength = builder.mMapXMinReadLength;
    mMapXMetaChunkSize = builder.mMapXMetaChunkSize;
    mMapXMetaChunkOverlap = builder.mMapXMetaChunkOverlap;
    mPairOrientation = builder.mPairOrientation;
    mParallelUnmatedProcessing = builder.mParallelUnmatedProcessing;
    mGapOpenPenalty = builder.mGapOpenPenalty;
    mGapExtendPenalty = builder.mGapExtendPenalty;
    mSubstitutionPenalty = builder.mSubstitutionPenalty;
    mUnknownsPenalty = builder.mUnknownsPenalty;
    mSoftClipDistance = builder.mSoftClipDistance;
    mAlignerBandWidthFactor = builder.mAlignerBandWidthFactor;
    mAlignerMode = builder.mAlignerMode;
    mSingleIndelPenalties = builder.mSingleIndelPenalties;
  }

  //  /**
  //   * gets the insert size
  //   * @return the value
  //   */
  //  public Integer expectedInsertSize() {
  //    return mInsertSize;
  //  }

  /**
   * gets the minimum fragment length
   * @return the value
   */
  public Integer minFragmentLength() {
    return mMinFragmentLength;
  }

  /**
   * gets the maximum fragment length
   * @return the value
   */
  public Integer maxFragmentLength() {
    return mMaxFragmentLength;
  }

  @Override
  @ParamsNoField
  public File directory() {
    return mOutputParams.directory();
  }

  /**
   * Returns whether output should be compressed.
   * @return wether to compress
   */
  @ParamsNoField
  public boolean compressOutput() {
    return mOutputParams.isCompressOutput();
  }

  /**
   * Is output block compressed.
   * @return true if output should be block compressed
   */
  @ParamsNoField
  public boolean blockCompressed() {
    return mOutputParams.isCompressOutput() && GzipAsynchOutputStream.BGZIP;
  }

  /**
   * Get the maximum count permissible for a hash.
   * @return the maximum hash count permissible.
   */
  public Integer hashCountThreshold() {
    return mHashCountThreshold;
  }

  /**
   * Whether the hash limit should be calculated from index data rather than as an explicit count.
   * @return true if yes
   */
  public boolean useProportionalHashThreshold() {
    return mUseProportionalHashThreshold;
  }

  /**
   * Get the maximum count permissible for a hash when using proportional threshold.
   * @return the maximum hash count permissible.
   */
  public Integer maxHashCountThreshold() {
    return mMaxHashCountThreshold;
  }

  /**
   * Get the minimum count permissible for a hash when using proportional threshold.
   * @return the minimum hash count permissible.
   */
  public Integer minHashCountThreshold() {
    return mMinHashCountThreshold;
  }

  /**
   * Gets the maximum number of hits a read may have before it is discarded.
   * @return the threshold
   */
  public int readFreqThreshold() {
    return mReadFreqThreshold;
  }

  /**
   * Get the number of threads to be used.
   * @return the number of threads to be used.
   */
  public int numberThreads() {
    return mNumberThreads;
  }

  /**
   * Gets the multiplier factor when dividing work into chunks among multiple threads.
   *
   * @return the number of chunks of work per thread.
   */
  public int threadMultiplier() {
    return mThreadMultiplier;
  }

  /**
   * @return the build and create parameters.
   */
  public ISequenceParams buildFirstParams() {
    return mBuildFirstParams;
  }

  /**
   * @return the build and create parameters.
   */
  public ISequenceParams buildSecondParams() {
    return mBuildSecondParams;
  }

  /**
   * Whether this params has paired reads
   * @return true if so
   */
  @ParamsNoField
  public boolean paired() {
    return mBuildSecondParams != null;
  }

  /**
   * @return the search parameters.
   */
  public ISequenceParams searchParams() {
    return mSearchParams;
  }

  /**
   * Get the listeners.
   * @return the listeners.
   */
  public Collection<ListenerType> listeners() {
    return mListeners;
  }

  /**
   * @return the output parameters.
   */
  public NgsOutputParams outputParams() {
    return mOutputParams;
  }
  /**
   * @return the mask parameters.
   */
  public NgsMaskParams maskParams() {
    return mMaskParams;
  }

  /**
   * Return step size for use in long read mapping.
   * Not set when using explicit masks
   * @return the value
   */
  public int stepSize() {
    return mStepSize;
  }

  /**
   * check whether parameters are suitable for using long read mapping
   * not set when using explicit masks
   * @return true if so.
   */
  public boolean useLongReadMapping() {
    return mUseLongReadMapping;
  }

  /**
   * get the protein scoring matrix
   * @return the protein scoring matrix
   */
  public ProteinScoringMatrix proteinScoringMatrix() {
    return mProteinScoringMatrix;
  }

  /**
   * Gets whether protein read cache should be used.
   *
   * @return true if protein read cache should be used.
   */
  public boolean enableProteinReadCache() {
    return mEnableProteinReadCache;
  }

  /**
   * Get the compress hashes flag.
   * @return the compress hashes flag.
   */
  public boolean compressHashes() {
    return mCompressHashes;
  }

  /**
   * @return <code>IntSet</code> window
   */
  public int intSetWindow() {
    return mIntSetWindow;
  }

  /**
   * If not using gapped output for long reads this parameter
   * determines how many hits to a region a read needs before it
   * is allowed to read output processing.
   * @return the number of hits, or null if not set
   */
  public Integer minHits() {
    return mMinHits;
  }

  /**
   * @return if legacy cigar format should be used
   */
  public boolean legacyCigars() {
    return mLegacyCigars;
  }

  /**
   * Return the sex of this individual.
   * @return the sex
   */
  @ParamsNoField
  public Sex sex() {
    return mSearchParams.sex();
  }

  /**
   * @return true if we should use top random
   */
  public boolean useTopRandom() {
    return mUseTopRandom;
  }

  /**
   * @return the minimum read length for a <code>mapX</code> run (in nucleotides). Reads shorter than this should be ignored.
   */
  public long mapXMinReadLength() {
    return mMapXMinReadLength;
  }

  /**
   * @return correct orientation for mating
   */
  public MachineOrientation pairOrientation() {
    return mPairOrientation;
  }

  /** @return true if parallelization of the mated+unmated processing stage is desired */
  public boolean parallelUnmatedProcessing() {
    return mParallelUnmatedProcessing;
  }

  /** @return the size at which meta chunks are created */
  public int mapXMetaChunkSize() {
    return mMapXMetaChunkSize;
  }

  /** @return how much overlap to have between adjacent meta chunks */
  public int mapXMetaChunkOverlap() {
    return mMapXMetaChunkOverlap;
  }

  /** @return the penalty for a gap open during alignment */
  public int gapOpenPenalty() {
    return mGapOpenPenalty;
  }

  /** @return the penalty for a gap extension during alignment */
  public int gapExtendPenalty() {
    return mGapExtendPenalty;
  }

  /** @return the penalty for a substitution during alignment */
  public int substitutionPenalty() {
    return mSubstitutionPenalty;
  }

  /** @return the penalty for an unknown nucleotide (or aligning off the of the template) during alignment */
  public int unknownsPenalty() {
    return mUnknownsPenalty;
  }

  /** @return the number of bases from the edge of alignments to inspect for indels, and soft clip if present */
  public int softClipDistance() {
    return mSoftClipDistance;
  }

  /** @return the maximum shift factor used during alignment */
  public MaxShiftFactor alignerBandWidthFactor() {
    return mAlignerBandWidthFactor;
  }

  /** @return the aligner chain to use */
  public AlignerMode alignerMode() {
    return mAlignerMode;
  }

  /** @return file containing single indel penalties or null if it wasn't specified */
  public String singleIndelPenalties() {
    return mSingleIndelPenalties;
  }

  @Override
  @ParamsNoField
  public File file(final String name) {
    return outputParams().file(name);
  }

  /**
   * This is not used except in tests - we can probably remove at some point
   * @return the stream for writing stuff.
   * @throws IOException whenever.
   */
  @ParamsNoField
  public OutputStream unusedOutStream() throws IOException {
    return paired() ? outputParams().matedSamStream() : outputParams().outStream();
  }

  @Override
  @ParamsNoField
  public void close() throws IOException {
    if (mBuildFirstParams != null) {
      mBuildFirstParams.close();
    }
    if (mBuildSecondParams != null) {
      mBuildSecondParams.close();
    }
    if (mSearchParams != null) {
      mSearchParams.close();
    }
  }

  /**
   * Check if queries are closed.
   * @return true iff the queries are currently closed.
   */
  @Override
  @ParamsNoField
  public boolean closed() {
    return mBuildFirstParams.closed() && mSearchParams.closed();
  }

  @Override
  @ParamsNoField
  public int hashCode() {
    return Utils.hash(new Object[] {mBuildFirstParams, mBuildSecondParams, mSearchParams, mHashCountThreshold, mNumberThreads, mOutputParams});
  }

  @Override
  @ParamsNoField
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    final NgsParams that = (NgsParams) obj;
    return Utils.equals(
        new Object[] {this.mBuildFirstParams, this.mBuildSecondParams, this.mSearchParams, this.mHashCountThreshold, this.mNumberThreads, this.mOutputParams},
        new Object[] {that.mBuildFirstParams, that.mBuildSecondParams, that.mSearchParams, that.mHashCountThreshold, that.mNumberThreads, that.mOutputParams}
        );
  }

  @Override
  public String toString() {
    final String linePrefix = com.rtg.util.StringUtils.LS + "..";
    return "NgsParams threshold=" + mHashCountThreshold + " compressHashes=" + mCompressHashes
        + " threads=" + mNumberThreads + " useLongRead=" + mUseLongReadMapping + linePrefix
        + " mapXMinLength=" + mMapXMinReadLength  + linePrefix
        + " search={" + mSearchParams + "} " + linePrefix
        + " output={" + outputParams() + "} " + linePrefix
        + (minFragmentLength() == null ? "" : " fragmentLength min=" + minFragmentLength() + " max=" + maxFragmentLength() + linePrefix)
        //  + (expectedInsertSize() == null ? "" : " expectedInsertSize=" + expectedInsertSize() + linePrefix)
        + " build={" + buildFirstParams() + "}" + linePrefix
        + (buildSecondParams() == null ? "" : " second={" + buildSecondParams() + "}" + linePrefix)
        + " search={" + searchParams() + "}"
        + " penalties(s,o,p)={" + substitutionPenalty() + "," + gapOpenPenalty() + "," + gapExtendPenalty() + "}"
        + " softClipDistance=" + softClipDistance()
        + " alignerMode=" + alignerMode()
        + com.rtg.util.StringUtils.LS;
  }

  @Override
  public boolean integrity() {
    if (mSearchParams == null) {
      throw new RuntimeException();
    }
    if (mBuildFirstParams == null) {
      throw new RuntimeException();
    }
    if (mOutputParams == null) {
      throw new RuntimeException();
    }
    if (mHashCountThreshold != null) {
      Exam.assertTrue(mUseProportionalHashThreshold || mHashCountThreshold >= 1);
    }
    Exam.assertTrue(mNumberThreads >= 1);
    //    Assert.assertTrue(mInsertSize == null || mInsertSize >= 1);
    if (mMaxFragmentLength == null) {
      Exam.assertTrue(mMinFragmentLength == null);
    } else {
      Exam.assertTrue(mMinFragmentLength != null && mMinFragmentLength >= 0 && mMaxFragmentLength >= mMinFragmentLength);
    }
    return true;
  }

  @Override
  public boolean globalIntegrity() {
    integrity();
    Exam.globalIntegrity(mSearchParams);
    Exam.globalIntegrity(mBuildFirstParams);
    Exam.globalIntegrity(mOutputParams);
    return true;
  }

  /**
   * Calculates how much template overlap is needed when chunking up template sub-jobs.
   *
   * @return the number of bases of overlap to use.
   */
  @ParamsNoField
  public int calculateThreadPadding() {
    long overlap;
    try {
      if (paired()) {
        //XXX this seems bogus - during alignment we can find longer indels than this and may require more padding than the mask params show.
        overlap = buildFirstParams().maxLength() * 2 + maxFragmentLength() + maskParams().getIndelLength() * maskParams().getIndels() * 2;
      } else {
        overlap = buildFirstParams().maxLength() + maskParams().getIndels() * maskParams().getIndelLength();
      }
    } catch (final UnsupportedOperationException e) {
      //XXX this catch is heinous, and seems to be solely to make up for particularly a particulary poor inheritance usage on explicit CG masks
      if (paired()) {
        overlap = buildFirstParams().maxLength() * 2 + maxFragmentLength() + 20;
      } else {
        overlap = buildFirstParams().maxLength() + 10;
      }
    }
    final int padding = (int) overlap;
    assert padding >= 0;
    return padding;
  }

  /**
   * Create a position params object from current values for running in long reads mode
   * @return params
   * @throws IOException If an I/O error occurs
   */
  @ParamsNoField
  public PositionParams toPositionParams() throws IOException {
    final NgsMaskParams maskP = maskParams();
    final int maxIndel = maskP.getIndels() * maskP.getIndelLength();
    int maxGap = (int) getMaxReadLength() - maskP.getWordSize() + maxIndel;
    if (maxGap == 0) {
      maxGap = 1;
    }
    final PositionDistributionParams pdp = new PositionDistributionParams(Double.NaN, Double.NaN, maxGap, maxIndel);
    final PositionOutputParams pop = new PositionOutputParams(outputParams().directory(), OutputFormatType.NGS, pdp, (double) (-outputParams().errorLimit()), outputParams().isCompressOutput(), outputParams().topN());
    final BuildParams template = BuildParams.builder().windowSize(maskP.getWordSize()).stepSize(1).sequences(mSearchParams).create();
    final BuildParams reads = BuildParams.builder().windowSize(maskP.getWordSize()).stepSize(mStepSize).sequences(mBuildFirstParams).compressHashes(compressHashes()).create();
    final BuildParams reads2;
    if (paired()) {
      reads2 = BuildParams.builder().windowSize(maskP.getWordSize()).stepSize(mStepSize).sequences(mBuildSecondParams).compressHashes(compressHashes()).create();
    } else {
      reads2 = null;
    }
    return PositionParams.builder().ngsParams(this).mode(ProgramMode.SLIMN).hashCountThreshold(mHashCountThreshold).buildParams(reads).buildSecondParams(reads2).searchParams(template).outputParams(pop).progress(outputParams().progress()).numberThreads(numberThreads()).create();
  }

  /**
   * @return the length of the longest read in the read sets
   */
  @ParamsNoField
  public long getMaxReadLength() {
    return paired() ? Math.max(buildFirstParams().maxLength(), buildSecondParams().maxLength()) : buildFirstParams().maxLength();
  }

  /**
   * Create a builder with all the values set to those of this object.
   * @return a builder
   */
  public NgsParamsBuilder cloneBuilder() {
    final NgsParamsBuilder npb = new NgsParamsBuilder();
    npb.name(name())
    .buildFirstParams(buildFirstParams())
    //        .expectedInsertSize(expectedInsertSize())
    .listeners(listeners())
    .maxFragmentLength(maxFragmentLength() == null ? 0 : maxFragmentLength())
    .minFragmentLength(minFragmentLength() == null ? 0 : minFragmentLength())
    .numberThreads(numberThreads())
    .outputParams(outputParams())
    .maskParams(maskParams())
    .proteinScoringMatrix(proteinScoringMatrix())
    .searchParams(searchParams())
    .buildSecondParams(buildSecondParams())
    .stepSize(stepSize())
    .hashCountThreshold(hashCountThreshold())
    .compressHashes(compressHashes())
    .useLongReadMapping(useLongReadMapping())
    .useProportionalHashThreshold(useProportionalHashThreshold())
    .useTopRandom(useTopRandom())
    .legacyCigars(legacyCigars())
    .minHits(minHits())
    .intsetWindow(intSetWindow())
    .enableProteinReadCache(enableProteinReadCache())
    .threadMultiplier(threadMultiplier())
    .readFreqThreshold(readFreqThreshold())
    .maxHashCountThreshold(maxHashCountThreshold())
    .minHashCountThreshold(minHashCountThreshold())
    .mapXMinLength(mapXMinReadLength())
    .mapXMetaChunkSize(mapXMetaChunkSize())
    .mapXMetaChunkOverlap(mapXMetaChunkOverlap())
    .pairOrientation(pairOrientation())
    .parallelUnmatedProcessing(parallelUnmatedProcessing())
    .gapOpenPenalty(gapOpenPenalty())
    .gapExtendPenalty(gapExtendPenalty())
    .substitutionPenalty(substitutionPenalty())
    .unknownsPenalty(unknownsPenalty())
    .softClipDistance(softClipDistance())
    .alignerBandWidthFactor(alignerBandWidthFactor())
    .alignerMode(alignerMode())
    .singleIndelPenalties(singleIndelPenalties());
    return npb;
  }
}
