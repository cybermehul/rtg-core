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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.rtg.alignment.AlignerMode;
import com.rtg.launcher.CommonFlags;
import com.rtg.launcher.DefaultReaderParams;
import com.rtg.launcher.ISequenceParams;
import com.rtg.launcher.SequenceParams;
import com.rtg.mode.SequenceMode;
import com.rtg.reader.AlternatingSequencesWriter;
import com.rtg.reader.FormatCli;
import com.rtg.reader.IndexFile;
import com.rtg.reader.InputFormat;
import com.rtg.reader.PrereadArm;
import com.rtg.reader.PrereadType;
import com.rtg.reader.ReaderUtils;
import com.rtg.reader.RightSimplePrereadNames;
import com.rtg.reader.SdfId;
import com.rtg.reader.SdfUtils;
import com.rtg.reader.SequenceDataSource;
import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesWriter;
import com.rtg.reader.SimplePrereadNames;
import com.rtg.reference.Sex;
import com.rtg.relation.GenomeRelationships;
import com.rtg.sam.SamCommandHelper;
import com.rtg.util.IntegerOrPercentage;
import com.rtg.util.InvalidParamsException;
import com.rtg.util.MaxShiftFactor;
import com.rtg.util.cli.CFlags;
import com.rtg.util.cli.Flag;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.ListenerType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.InputFileUtils;
import com.rtg.util.machine.MachineType;

import net.sf.samtools.FileTruncatedException;
import net.sf.samtools.SAMException;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.util.RuntimeEOFException;
import net.sf.samtools.util.RuntimeIOException;

/**
 * Populate parameter objects for Map commands
 */
public final class MapParamsHelper {

  private MapParamsHelper() { }

  static long populateCommonMapParams(NgsParamsBuilder ngsParamsBuilder, CFlags flags, int defWordSize, int defStepRatio, boolean includeNames, boolean includeFullNames) throws InvalidParamsException, IOException {
    final int numberThreads = CommonFlags.parseThreads((Integer) flags.getValue(CommonFlags.THREADS_FLAG));

    final NameParams nameParams = new NameParams(includeNames, includeFullNames);
    final long maxReadLength = populateReaders(ngsParamsBuilder, flags, nameParams);
    if (ngsParamsBuilder.mUseLongReadMapping) {
      MapParamsHelper.addLongReadParameters(flags, ngsParamsBuilder, maxReadLength, defWordSize, defStepRatio);
    }

    if (ngsParamsBuilder.mStepSize > maxReadLength) {
      throw new InvalidParamsException("Step size (" + ngsParamsBuilder.mStepSize + ") must be less than or equal to max read length (" + maxReadLength + ")");
    }

    final Flag alignerChain = flags.getFlag(MapFlags.ALIGNER_MODE_FLAG);
    if (alignerChain != null) {
      ngsParamsBuilder.alignerMode((AlignerMode) alignerChain.getValue());
    }
    final Flag singleIndelPenalties = flags.getFlag(MapFlags.SINGLE_INDEL_PENALTIES_FLAG);
    if (singleIndelPenalties != null) {
      ngsParamsBuilder.singleIndelPenalties((String) singleIndelPenalties.getValue());
    }

    final Collection<ListenerType> listeners = new HashSet<>();
    listeners.add(ListenerType.CLI);
    final IntegerOrPercentage repeat = (IntegerOrPercentage) flags.getValue(CommonFlags.REPEAT_FREQUENCY_FLAG);
    ngsParamsBuilder.listeners(listeners)
                    .numberThreads(numberThreads)
                    .threadMultiplier((Integer) flags.getValue(MapFlags.THREAD_MULTIPLIER))
                    .useProportionalHashThreshold(repeat.isPercentage())
                    .readFreqThreshold((Integer) flags.getValue(MapFlags.READ_FREQUENCY_FLAG))
                    .legacyCigars(flags.isSet(MapFlags.LEGACY_CIGARS))
                    .maxHashCountThreshold((Integer) flags.getValue(CommonFlags.MAX_REPEAT_FREQUENCY_FLAG))
                    .minHashCountThreshold((Integer) flags.getValue(CommonFlags.MIN_REPEAT_FREQUENCY_FLAG))
                    .parallelUnmatedProcessing((Boolean) flags.getValue(CommonFlags.PARALLEL_UNMATED_PROCESSING_FLAG));
    if (repeat.isPercentage()) {
      ngsParamsBuilder.hashCountThreshold(100 - repeat.getValue(100));
    } else {
      ngsParamsBuilder.hashCountThreshold(repeat.getRawValue());
    }
    return maxReadLength;
  }

  static void populateAlignmentScoreSettings(CFlags flags, NgsFilterParams.NgsFilterParamsBuilder ngsFilterParamsBuilder, boolean paired, SAMReadGroupRecord rg)  {
    final boolean isIonTorrent = rg != null && MachineType.IONTORRENT.compatiblePlatform(rg.getPlatform());
    if (isIonTorrent) {
      Diagnostic.developerLog("IonTorrent mode enabled");
    }
    final IntegerOrPercentage ionTorrentDefaultThreshold = new IntegerOrPercentage("10%");
    if (paired) {
      final IntegerOrPercentage matedAS;
      if (flags.isSet(CommonFlags.MATED_MISMATCH_THRESHOLD)) {
        matedAS = (IntegerOrPercentage) flags.getValue(CommonFlags.MATED_MISMATCH_THRESHOLD);
      } else {
        if (!flags.isSet(CommonFlags.MAX_ALIGNMENT_MISMATCHES) && isIonTorrent) {
          matedAS = ionTorrentDefaultThreshold;
        } else {
          matedAS = (IntegerOrPercentage) flags.getValue(CommonFlags.MAX_ALIGNMENT_MISMATCHES);  //the defaults for mated alignment threshold and max align score are supposed to be the same
        }
      }
      final IntegerOrPercentage unmatedAS;
      if (flags.isSet(CommonFlags.UNMATED_MISMATCH_THRESHOLD) || !isIonTorrent) { //if flag is set OR not iontorrent
        unmatedAS = (IntegerOrPercentage) flags.getValue(CommonFlags.UNMATED_MISMATCH_THRESHOLD);
      } else {  // ie flag is not set AND this is iontorrent
        unmatedAS = ionTorrentDefaultThreshold;
      }
      if (flags.isSet(CommonFlags.UNMATED_MISMATCH_THRESHOLD) && unmatedAS.compareTo(matedAS) > 0) {
        Diagnostic.warning("--" + CommonFlags.UNMATED_MISMATCH_THRESHOLD + " should not be greater than --" + CommonFlags.MATED_MISMATCH_THRESHOLD);
      }
      ngsFilterParamsBuilder.unmatedMaxMismatches(unmatedAS);
      ngsFilterParamsBuilder.matedMaxMismatches(matedAS);
    } else {
      if (!flags.isSet(CommonFlags.MAX_ALIGNMENT_MISMATCHES) && isIonTorrent) {
        ngsFilterParamsBuilder.matedMaxMismatches(ionTorrentDefaultThreshold).unmatedMaxMismatches(ionTorrentDefaultThreshold);
      } else {
        ngsFilterParamsBuilder.matedMaxMismatches((IntegerOrPercentage) flags.getValue(CommonFlags.MAX_ALIGNMENT_MISMATCHES))
              .unmatedMaxMismatches((IntegerOrPercentage) flags.getValue(CommonFlags.MAX_ALIGNMENT_MISMATCHES));
      }
    }
  }

  static NgsParams createAndValidate(NgsParamsBuilder ngsParamsBuilder) throws IOException {
    final NgsParams localParams = ngsParamsBuilder.create();
    if (localParams.paired()) {
      if (localParams.buildFirstParams().reader().hasNames() != localParams.buildSecondParams().reader().hasNames()) {
        if (localParams.outputParams().outputReadNames()) {
          throw new InvalidParamsException("Names only present on one arms SDF and read names requested");
        }
      }
    }
    if (localParams.outputParams().outputReadNames() && !localParams.buildFirstParams().reader().hasNames()) {
      throw new InvalidParamsException("Names not present in SDF and read names requested");
    }
    if (localParams.outputParams().calibrateRegions() != null) {
      localParams.outputParams().calibrateRegions().validateTemplate(localParams.searchParams().reader());
    }

    localParams.globalIntegrity();
    return localParams;
  }

  static boolean isPaired(CFlags flags) {
    final String format;
    final String qualityFormat;
    final InputFormat inFormat;
    if (flags.isSet(FormatCli.FORMAT_FLAG)) {
      format = flags.getValue(FormatCli.FORMAT_FLAG).toString().toLowerCase(Locale.getDefault());
      qualityFormat = flags.isSet(FormatCli.QUALITY_FLAG) ? flags.getValue(FormatCli.QUALITY_FLAG).toString().toLowerCase(Locale.getDefault()) : null;
    } else {
      format = MapFlags.SDF_FORMAT;
      qualityFormat = null;
    }
    if (format.equals(MapFlags.SDF_FORMAT)) {
      inFormat = null;
    } else {
      inFormat = FormatCli.getFormat(format, qualityFormat, true);
    }
    final boolean sdf = format.equals(MapFlags.SDF_FORMAT);
    final File reads = (File) flags.getValue(CommonFlags.READS_FLAG);
    final boolean paired;
    if (sdf) {
      paired = ReaderUtils.isPairedEndDirectory(reads);
    } else {
      paired = !flags.isSet(CommonFlags.READS_FLAG) || inFormat == InputFormat.SAM_PE;
    }
    return paired;
  }

  /**
   *
   * @param ngsParamsBuilder builder to populate
   * @param flags flags to get configuration from
   * @return read length value
   * @throws InvalidParamsException when stuff goes wrong
   * @throws IOException when other stuff goes wrong
   */
  private static long populateReaders(NgsParamsBuilder ngsParamsBuilder, CFlags flags, NameParams nameParams) throws InvalidParamsException, IOException {
    final boolean paired = initReaders(ngsParamsBuilder, flags, nameParams, true, SequenceMode.BIDIRECTIONAL, SequenceMode.UNIDIRECTIONAL, new SamSequenceReaderParams(false, true));
    final long maxReadLength;
    final boolean useLongReads;
    if (!paired) {
      final ISequenceParams params = ngsParamsBuilder.buildFirstParams();
      maxReadLength = params.reader().maxLength();
      final long minReadLength = params.reader().minLength();
      useLongReads = flags.isSet(CommonFlags.FORCE_LONG_FLAG)
              || maxReadLength > 63
              || (minReadLength != maxReadLength);
      Diagnostic.userLog("Entering single end read mode read length=" + maxReadLength);
    } else {
      final ISequenceParams leftParams = ngsParamsBuilder.buildFirstParams();
      final ISequenceParams rightParams = ngsParamsBuilder.buildSecondParams();
      final long leftReadLength = leftParams.reader().maxLength();
      final long rightReadLength = rightParams.reader().maxLength();
      maxReadLength = Math.max(leftReadLength, rightReadLength);
      final long minReadLength = Math.min(leftParams.reader().minLength(), rightParams.reader().minLength());
      useLongReads = flags.isSet(CommonFlags.FORCE_LONG_FLAG)
              || (maxReadLength > 63)
              || (minReadLength != maxReadLength);
      Diagnostic.userLog("Entering paired end read mode 1st arm read length=" + leftReadLength + " 2nd arm read length=" + rightReadLength);
    }

    if (maxReadLength == 0) {
      throw new InvalidParamsException(ErrorType.INFO_ERROR, "Read length must be greater than 0");
    }

    ngsParamsBuilder.useLongReadMapping(useLongReads);
    return maxReadLength;
  }

  /**
   * Initialise sequences readers
   * @param ngsParamsBuilder builder to populate
   * @param flags flags to get configuration from
   * @param nameParams options for read name loading
   * @param useQuality true to read quality when using FASTQ
   * @param templateMode the template sequence mode
   * @param readsMode the reads sequence mode
   * @param samParams parameters for when reading sequence data from SAM or BAM files
   * @return true if paired, false otherwise
   * @throws InvalidParamsException when stuff goes wrong
   * @throws IOException when other stuff goes wrong
   */
  public static boolean initReaders(NgsParamsBuilder ngsParamsBuilder, CFlags flags, NameParams nameParams, boolean useQuality, SequenceMode templateMode, SequenceMode readsMode, SamSequenceReaderParams samParams) throws InvalidParamsException, IOException {
    final String format;
    final String qualityFormat;
    final InputFormat inFormat;
    if (flags.isSet(FormatCli.FORMAT_FLAG)) {
      format = flags.getValue(FormatCli.FORMAT_FLAG).toString().toLowerCase(Locale.getDefault());
      qualityFormat = flags.isSet(FormatCli.QUALITY_FLAG) ? flags.getValue(FormatCli.QUALITY_FLAG).toString().toLowerCase(Locale.getDefault()) : null;
    } else {
      format = MapFlags.SDF_FORMAT;
      qualityFormat = null;
    }
    final boolean sdf = format.equals(MapFlags.SDF_FORMAT);
    if (sdf) {
      inFormat = null;
    } else {
      inFormat = FormatCli.getFormat(format, qualityFormat, useQuality);
    }
    final File reads = (File) flags.getValue(CommonFlags.READS_FLAG);
    final boolean paired;
    if (sdf) {
      paired = ReaderUtils.isPairedEndDirectory(reads);
    } else {
      paired = !flags.isSet(CommonFlags.READS_FLAG) || inFormat == InputFormat.SAM_PE;
    }
    if (!paired) {
      if (sdf) {
        makeSequenceParamsMulti(ngsParamsBuilder, flags, reads, null, nameParams, templateMode, readsMode);
      } else {
        makeOnTheFlySequenceParamsMulti(ngsParamsBuilder, flags, inFormat, new File[]{reads, null}, nameParams, useQuality, templateMode, readsMode, samParams);
      }
      final ISequenceParams params = ngsParamsBuilder.buildFirstParams();
      if (params.reader().getPrereadType() == PrereadType.CG) {
        throw new InvalidParamsException(ErrorType.IS_A_CG_SDF, reads.getPath());
      }
    } else {
      if (sdf) {
        final File left = ReaderUtils.getLeftEnd(reads);
        final File right = ReaderUtils.getRightEnd(reads);
        makeSequenceParamsMulti(ngsParamsBuilder, flags, left, right, nameParams, templateMode, readsMode);
      } else if (inFormat == InputFormat.SAM_PE) {
        makeOnTheFlySequenceParamsMulti(ngsParamsBuilder, flags, inFormat, new File[]{reads, null}, nameParams, useQuality, templateMode, readsMode, samParams);
      } else {
        final File leftFile = (File) flags.getValue(FormatCli.LEFT_FILE_FLAG);
        final File rightFile = (File) flags.getValue(FormatCli.RIGHT_FILE_FLAG);
        if (InputFileUtils.checkIdenticalPaths(leftFile, rightFile)) {
          throw new InvalidParamsException("Paths given for --" + FormatCli.LEFT_FILE_FLAG + " and --" + FormatCli.RIGHT_FILE_FLAG + " are the same file.");
        }
        makeOnTheFlySequenceParamsMulti(ngsParamsBuilder, flags, inFormat, new File[] {leftFile, rightFile}, nameParams, useQuality, templateMode, readsMode, samParams);
      }
      final ISequenceParams leftParams = ngsParamsBuilder.buildFirstParams();
      final ISequenceParams rightParams = ngsParamsBuilder.buildSecondParams();
      if (leftParams.numberSequences() != rightParams.numberSequences()) {
        throw new InvalidParamsException("Left and right SDFs for read pair must have same number of sequences, actually had: "
                + leftParams.numberSequences() + " and " + rightParams.numberSequences());
      }
      if ((leftParams.reader().getPrereadType() == PrereadType.CG)
              || (rightParams.reader().getPrereadType() == PrereadType.CG)) {
        throw new InvalidParamsException(ErrorType.IS_A_CG_SDF, reads.getPath());
      }
    }
    return paired;
  }

  static NgsMaskParams makeMaskParams(final CFlags flags, final int readLength, boolean useLongReads, int defWordSize) {
    final NgsMaskParams maskParams;
    if (flags.isSet(CommonFlags.MASK_FLAG)) {
      maskParams = new NgsMaskParamsExplicit((String) flags.getValue(CommonFlags.MASK_FLAG));
    } else {
      final int w = CommonFlags.getWordSize(flags, readLength, defWordSize);
      final int a = (Integer) flags.getValue(CommonFlags.SUBSTITUTIONS_FLAG);
      final int b = (Integer) flags.getValue(CommonFlags.INDELS_FLAG);
      final int c = (Integer) flags.getValue(CommonFlags.INDEL_LENGTH_FLAG);
      final int s = Math.max(a, b);
      if (readLength < w) {
        throw new InvalidParamsException(ErrorType.WORD_NOT_LESS_READ, w + "", readLength + "");
      }
      if (b > 0 && c > readLength - w) {
        throw new InvalidParamsException(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "-c", c + "", (readLength - w) + "");
      }
      if (b > 0 && c <= 0) {
        throw new InvalidParamsException(ErrorType.INVALID_MIN_INTEGER_FLAG_VALUE, "-c", c + "", "1");
      }
      if (b > readLength - w) {
        throw new InvalidParamsException(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "-b", b + "", (readLength - w) + "");
      }
      if (s > readLength - w) {
        throw new InvalidParamsException(ErrorType.INVALID_MAX_INTEGER_FLAG_VALUE, "-a", s + "", (readLength - w) + "");
      }
      if (useLongReads) {
        maskParams = new LongReadMaskParams(w, s, b, c);
      } else {
        maskParams = new NgsMaskParamsGeneral(w, s, b, c, false /* no CG data here */);
      }
      if (!maskParams.isValid(readLength)) {
        throw new InvalidParamsException(ErrorType.INVALID_MASK_PARAMS);
      }
    }
    return maskParams;
  }

  private static void addLongReadParameters(final CFlags flags, final NgsParamsBuilder ngsParamsBuilder, final long readLength, final int defWordSize, int defStepRatio) {
    final int w = CommonFlags.getWordSize(flags, (int) readLength, defWordSize);
    final int subs = (Integer) flags.getValue(CommonFlags.SUBSTITUTIONS_FLAG);
    if (!((readLength / w) >= subs + 1)) {
      throw new InvalidParamsException(ErrorType.INVALID_LONG_READ_PARAMS, Integer.toString(w), Integer.toString(subs));
    }
    final int step;
    if (flags.isSet(CommonFlags.STEP_FLAG)) {
      step = (Integer) flags.getValue(CommonFlags.STEP_FLAG);
    } else {
      step = Math.max(w / defStepRatio, 1);
    }
    ngsParamsBuilder.stepSize(step);
  }


  private static FutureTask<SequenceParams> getTemplateFutureTask(NgsParamsBuilder ngsParamsBuilder, CFlags flags, boolean includeFullNames, SequenceMode templateMode) throws IOException {
    final boolean templateMem = !flags.isSet(MapFlags.NO_INMEMORY_TEMPLATE);
    final File template = (File) flags.getValue(MapFlags.TEMPLATE_FLAG);
    final Sex sex = getMappingSex(ngsParamsBuilder, flags);
    return new FutureTask<>(new SequenceParamsCallableSdf(template, LongRange.NONE, templateMem, sex, includeFullNames, templateMode));
  }

  private static Sex getMappingSex(NgsParamsBuilder ngsParamsBuilder, CFlags flags) throws IOException {
    final Sex sex;
    if (flags.isSet(MapFlags.PEDIGREE_FLAG)) {
      final SAMReadGroupRecord rg = ngsParamsBuilder.mOutputParams.readGroup();
      if (rg == null) {
        throw new InvalidParamsException("No read group information has been provided, so cannot obtain sex information from pedigree file.");
      }
      final String sample = rg.getSample();
      if (sample == null) {
        throw new InvalidParamsException("Supplied read group information does not contain a sample, so cannot obtain sex information from pedigree file.");
      }
      final File relfile = (File) flags.getValue(MapFlags.PEDIGREE_FLAG);
      final GenomeRelationships gr = GenomeRelationships.loadGenomeRelationships(relfile);
      if (!gr.hasGenome(sample)) {
        throw new InvalidParamsException("Supplied pedigree file does not contain sample " + sample);
      }
      sex = gr.getSex(sample);
      Diagnostic.userLog("Identified sex of sample " + sample + " from pedigree as " + sex);
    } else {
      sex = flags.isSet(MapFlags.SEX_FLAG) ? (Sex) flags.getValue(MapFlags.SEX_FLAG) : null;
    }
    return sex;
  }

  /**
   * Loads template and reads (left and right if paired end) in parallel.
   * @throws IOException if an IO problem occurs.
   */
  private static void makeSequenceParamsMulti(final NgsParamsBuilder ngsParamsBuilder, CFlags flags, final File build, final File buildSecond, NameParams nameParams, SequenceMode templateMode, SequenceMode readsMode) throws InvalidParamsException, IOException {
    final LongRange buildRegion = CommonFlags.getReaderRestriction(flags);
    final ExecutorService executor = Executors.newFixedThreadPool(3);
    try {
      final FutureTask<SequenceParams> leftTask = new FutureTask<>(new SequenceParamsCallableSdf(build, buildRegion, nameParams, readsMode));
      executor.execute(leftTask);

      try {
        FutureTask<SequenceParams> rightTask = null;
        if (buildSecond != null) {
//        final HashingRegion adjustedRegion = leftTask.get().readerParams().adjustedRegion();
//        final HashingRegion secondRegion = adjustedRegion == null ? buildRegion : adjustedRegion;
          rightTask = new FutureTask<>(new SequenceParamsCallableSdf(buildSecond, buildRegion, nameParams.includeFullNames() ? nameParams : new NameParams(false, false), readsMode));
          executor.execute(rightTask);
        }

        final FutureTask<SequenceParams> templateTask = getTemplateFutureTask(ngsParamsBuilder, flags, nameParams.includeFullNames(), templateMode);
        executor.execute(templateTask);

        ngsParamsBuilder.buildFirstParams(leftTask.get());
        if (rightTask != null) {
          ngsParamsBuilder.buildSecondParams(rightTask.get());
        }
        ngsParamsBuilder.searchParams(templateTask.get());
      } catch (final ExecutionException ie) {
        if (ie.getCause() instanceof NoTalkbackSlimException) {
          throw (NoTalkbackSlimException) ie.getCause();
        } else if (ie.getCause() instanceof IOException) {
          throw (IOException) ie.getCause();
        } else if (ie.getCause() instanceof SAMException) {
          if (ie.getCause() instanceof RuntimeIOException
                  || ie.getCause() instanceof RuntimeEOFException
                  || ie.getCause() instanceof FileTruncatedException) {
            throw new IOException(ie.getCause().getMessage(), ie.getCause());
          }
          throw new NoTalkbackSlimException(ie.getCause(), ErrorType.SAM_BAD_FORMAT_NO_FILE, ie.getCause().getMessage());
        } else if (ie.getCause() instanceof Error) {
          throw (Error) ie.getCause();
        }
        throw new RuntimeException(ie.getCause());
      } catch (final InterruptedException ie) {
        throw new InvalidParamsException(ErrorType.INFO_ERROR, "Interrupted while loading datasets.");
      }
    } finally {
      executor.shutdownNow();
      while (!executor.isTerminated()) {
        try {
          executor.awaitTermination(1L, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
          break;
        }
      }
    }
  }

  /**
   * Loads template and reads (left and right if paired end) in parallel.
   * @throws IOException if an IO problem occurs.
   */
  private static void makeOnTheFlySequenceParamsMulti(NgsParamsBuilder ngsParamsBuilder, CFlags flags, InputFormat format, File[] builds, NameParams nameParams, boolean useQuality, SequenceMode templateMode, SequenceMode readsMode, SamSequenceReaderParams samParams) throws InvalidParamsException, IOException {
    assert builds.length == 2;
    //TODO icky way of getting within the 8 param limit
    final File build = builds[0];
    final File buildSecond = builds[1];
    final FutureTask<SequenceParams> templateTask = getTemplateFutureTask(ngsParamsBuilder, flags, nameParams.includeFullNames(), templateMode);
    final LongRange buildRegion = CommonFlags.getReaderRestriction(flags);
    final ExecutorService executor = Executors.newFixedThreadPool(3);
    try {

      executor.execute(templateTask);

      try {
        final SimplePrereadNames names = nameParams.includeNames() ? new SimplePrereadNames() : null;
        final SimplePrereadNames suffixes = nameParams.includeFullNames() ? new SimplePrereadNames() : null;

        if (format == InputFormat.SAM_PE || format == InputFormat.SAM_SE) {
          final FutureTask<SequenceParams[]> samTask = new FutureTask<>(new SequenceParamsCallableSam(build, format, buildRegion, names, suffixes, useQuality, readsMode, samParams));
          executor.execute(samTask);
          final SequenceParams[] sp = samTask.get();
          assert sp.length == 2;
          ngsParamsBuilder.buildFirstParams(sp[0]);
          if (sp[1] != null) {
            ngsParamsBuilder.buildSecondParams(sp[1]);
          }
        } else {
          final FutureTask<SequenceParams> leftTask = new FutureTask<>(new SequenceParamsCallableFasta(build, format, buildRegion, buildSecond != null ? PrereadArm.LEFT : null, names, suffixes, useQuality, readsMode));
          executor.execute(leftTask);
          FutureTask<SequenceParams> rightTask = null;
          if (buildSecond != null) {
            final RightSimplePrereadNames rNames = names == null ? null : new RightSimplePrereadNames(names);
            final RightSimplePrereadNames rSuffixes = names == null ? null : new RightSimplePrereadNames(suffixes);
            rightTask = new FutureTask<>(new SequenceParamsCallableFasta(buildSecond, format, buildRegion, PrereadArm.RIGHT, rNames, rSuffixes, useQuality, readsMode));
            executor.execute(rightTask);
          }
          ngsParamsBuilder.buildFirstParams(leftTask.get());
          if (rightTask != null) {
            ngsParamsBuilder.buildSecondParams(rightTask.get());
          }
        }
        ngsParamsBuilder.searchParams(templateTask.get());
      } catch (final ExecutionException ie) {
        if (ie.getCause() instanceof NoTalkbackSlimException) {
          throw (NoTalkbackSlimException) ie.getCause();
        } else if (ie.getCause() instanceof IOException) {
          throw (IOException) ie.getCause();
        } else if (ie.getCause() instanceof SAMException) {
          if (ie.getCause() instanceof RuntimeIOException
                  || ie.getCause() instanceof RuntimeEOFException
                  || ie.getCause() instanceof FileTruncatedException) {
            throw new IOException(ie.getCause().getMessage(), ie.getCause());
          }
          throw new NoTalkbackSlimException(ie.getCause(), ErrorType.SAM_BAD_FORMAT_NO_FILE, ie.getCause().getMessage());
        } else if (ie.getCause() instanceof Error) {
          throw (Error) ie.getCause();
        }
        throw new RuntimeException(ie.getCause());
      } catch (final InterruptedException ie) {
        throw new InvalidParamsException(ErrorType.INFO_ERROR, "Interrupted while loading datasets.");
      }
    } finally {
      executor.shutdownNow();
      while (!executor.isTerminated()) {
        try {
          executor.awaitTermination(1L, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
          break;
        }
      }
    }
  }

  /**
   * Retrieve the sam read group from somewhere within the flags object.
   * @param flags the flags object
   * @return the best sam read group record available from the flags
   * @throws IOException when reading one of the flag files fall
   */
  public static SAMReadGroupRecord getSAMReadGroupRecord(CFlags flags) throws IOException {
    final SAMReadGroupRecord rg;
    if (flags.isSet(SamCommandHelper.SAM_RG)) {
      final String readGroupString = (String) flags.getValue(SamCommandHelper.SAM_RG);
      rg = SamCommandHelper.validateAndCreateSamRG(readGroupString, SamCommandHelper.ReadGroupStrictness.REQUIRED);

    } else {
      final File reads = (File) flags.getValue(CommonFlags.READS_FLAG);
      if (!flags.isSet(FormatCli.FORMAT_FLAG)) {
        final File sdf;
        if (ReaderUtils.isPairedEndDirectory(reads)) {
          sdf = ReaderUtils.getLeftEnd(reads);
        } else {
          sdf = reads;
        }
        final IndexFile index = new IndexFile(sdf);
        final String readGroupString = index.getSamReadGroup();
        if (readGroupString != null) {
          rg = SamCommandHelper.validateAndCreateSamRG(readGroupString.replaceAll("\t", "\\\\t"), SamCommandHelper.ReadGroupStrictness.REQUIRED);
        } else {
          rg = null;
        }
      } else if (SamCommandHelper.isSamInput(flags) && reads.isFile()) {
        // Only try if it is a regular file (not a pipe which can't be opened twice)
        rg = SamCommandHelper.validateAndCreateSamRG(reads.getPath(), SamCommandHelper.ReadGroupStrictness.OPTIONAL);
      } else {
        rg = null;
      }
    }
    return rg;
  }

  static final class SequenceParamsCallableSdf implements Callable<SequenceParams> {
    private final File mBuild;
    private final boolean mUseMemReader;
    private final boolean mReads;
    private final LongRange mReaderRestriction;
    private final Sex mSex;
    private final boolean mIncludeReadNames;
    private final boolean mIncludeFullNames;
    private final SequenceMode mMode;

    SequenceParamsCallableSdf(final File build, final LongRange readerRestriction, NameParams nameParams, SequenceMode mode) { // C'tor for reads
      this(build, true, true, readerRestriction, null, nameParams.includeNames(), nameParams.includeFullNames(), mode);
    }

    SequenceParamsCallableSdf(File build, LongRange readerRestriction, boolean useMemReader, Sex sex, boolean includeFullNames, SequenceMode mode) { // C'tor for template
      this(build, useMemReader, false, readerRestriction, sex, false, includeFullNames, mode);
    }

    private SequenceParamsCallableSdf(File build, boolean useMemReader, boolean reads, LongRange readerRestriction, Sex sex, boolean includeReadNames, boolean includeFullNames, SequenceMode mode) {
      mBuild = build;
      mUseMemReader = useMemReader;
      mReads = reads;
      mReaderRestriction = readerRestriction;
      mSex = sex;
      mIncludeReadNames = includeReadNames;
      mIncludeFullNames = includeFullNames;
      mMode = mode;
    }

    @Override
    public SequenceParams call() {
      if (mReads) {
        return SequenceParams.builder().directory(mBuild).useMemReader(mUseMemReader).loadNames(mIncludeReadNames).loadFullNames(mIncludeFullNames).mode(mMode).readerRestriction(mReaderRestriction).create(); // Reads
      } else {
        SdfUtils.validateHasNames(mBuild);
        final SequenceParams params = SequenceParams.builder().sex(mSex).directory(mBuild).useMemReader(mUseMemReader).loadNames(true).loadFullNames(mIncludeFullNames).mode(mMode).readerRestriction(mReaderRestriction).create();
        try {
          SdfUtils.validateNoDuplicates(params, !mReads);
          return params; // Template
        } catch (final RuntimeException e) {
          try {
            params.close();
          } catch (final IOException e1) { }
          throw e;
        }
      }
    }
  }

  static final class SequenceParamsCallableFasta implements Callable<SequenceParams> {
    private final File mBuild;
    private final boolean mUseMemReader;
    private final InputFormat mInputFormat;
    private final LongRange mReaderRestriction;
    private final SimplePrereadNames mNames;
    private final SimplePrereadNames mSuffixes;
    private final PrereadArm mArm;
    private final SequenceMode mMode;
    private final boolean mUseQuality;

    SequenceParamsCallableFasta(File build, InputFormat format, LongRange readerRestriction, PrereadArm arm, SimplePrereadNames names, SimplePrereadNames suffixes, boolean useQuality, SequenceMode mode) { // C'tor for reads

      mBuild = build;
      mUseMemReader = true;
      mReaderRestriction = readerRestriction;
      mInputFormat = format;
      mNames = names;
      mSuffixes = suffixes;
      mArm = arm;
      mMode = mode;
      mUseQuality = useQuality;
    }

    @Override
    public SequenceParams call() throws IOException {
      final SequenceDataSource ds = FormatCli.getDnaDataSource(Arrays.asList(mBuild), mInputFormat, mArm, false, false, null, false);
      final SequencesWriter sw = new SequencesWriter(ds, null, PrereadType.UNKNOWN, true);
      sw.setSdfId(new SdfId(0));
      final SequencesReader reader = sw.processSequencesInMemory(mBuild, mUseQuality, mNames, mSuffixes,  mReaderRestriction);
      return SequenceParams.builder().readerParam(new DefaultReaderParams(reader, mReaderRestriction, mMode)).useMemReader(mUseMemReader).mode(mMode).readerRestriction(mReaderRestriction).create(); // Reads
    }
  }

  static final class SequenceParamsCallableSam implements Callable<SequenceParams[]> {
    private final File mBuild;
    private final boolean mUseMemReader;
    private final InputFormat mInputFormat;
    private final LongRange mReaderRestriction;
    private final SimplePrereadNames mNames;
    private final SimplePrereadNames mSuffixes;
    private final SequenceMode mMode;
    private final boolean mUseQuality;
    private final SamSequenceReaderParams mSamParams;

    SequenceParamsCallableSam(File build, InputFormat format, LongRange readerRestriction, SimplePrereadNames names, SimplePrereadNames suffixes, boolean useQuality, SequenceMode mode, SamSequenceReaderParams samParams) { // C'tor for reads
      mBuild = build;
      mUseMemReader = true;
      mReaderRestriction = readerRestriction;
      mInputFormat = format;
      mNames = names;
      mSuffixes = suffixes;
      mMode = mode;
      mUseQuality = useQuality;
      mSamParams = samParams;
    }

    @Override
    public SequenceParams[] call() throws Exception {
      final SequenceDataSource ds = FormatCli.getDnaDataSource(Arrays.asList(mBuild), mInputFormat, null, mSamParams.unorderedLoad(), mSamParams.flattenPairs(), null, false);
      final SequencesReader[] readers;
      if (mInputFormat == InputFormat.SAM_PE) {
        final AlternatingSequencesWriter asw = new AlternatingSequencesWriter(ds, null, PrereadType.UNKNOWN, true);
        asw.setSdfId(new SdfId(0));
        asw.setCheckDuplicateNames(true);
        readers = asw.processSequencesInMemoryPaired(mBuild, mUseQuality, mNames, mSuffixes, mReaderRestriction);
      } else {
        final SequencesWriter sw = new SequencesWriter(ds, null, PrereadType.UNKNOWN, true);
        sw.setSdfId(new SdfId(0));
        sw.setCheckDuplicateNames(true);
        readers = new SequencesReader[] {sw.processSequencesInMemory(mBuild, mUseQuality, mNames, mSuffixes, mReaderRestriction), null};
      }
      final SequenceParams[] sp = new SequenceParams[readers.length];
      for (int i = 0; i < readers.length; i++) {
        if (readers[i] != null) {
          sp[i] = SequenceParams.builder().readerParam(new DefaultReaderParams(readers[i], mReaderRestriction, mMode)).useMemReader(mUseMemReader).mode(mMode).readerRestriction(mReaderRestriction).create(); // Reads
        }
      }
      return sp;
    }
  }

  static NgsParamsBuilder populateAlignerPenaltiesParams(NgsParamsBuilder ngsParamsBuilder, CFlags flags) {
    ngsParamsBuilder.gapOpenPenalty((Integer) flags.getValue(MapFlags.GAP_OPEN_PENALTY_FLAG));
    ngsParamsBuilder.gapExtendPenalty((Integer) flags.getValue(MapFlags.GAP_EXTEND_PENALTY_FLAG));
    ngsParamsBuilder.substitutionPenalty((Integer) flags.getValue(MapFlags.MISMATCH_PENALTY_FLAG));
    ngsParamsBuilder.unknownsPenalty((Integer) flags.getValue(MapFlags.UNKNOWNS_PENALTY_FLAG));
    ngsParamsBuilder.softClipDistance((Integer) flags.getValue(MapFlags.SOFT_CLIP_DISTANCE_FLAG));
    ngsParamsBuilder.alignerBandWidthFactor(new MaxShiftFactor((Double) flags.getValue(MapFlags.ALIGNER_BAND_WIDTH_FACTOR_FLAG)));
    return ngsParamsBuilder;
  }
}
