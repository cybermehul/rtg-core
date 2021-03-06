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

package com.rtg.variant.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.rtg.tabix.TabixIndexReader;
import com.rtg.tabix.TabixIndexer;
import com.rtg.util.Pair;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.ErrorType;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.vcf.VcfUtils;
import com.rtg.vcf.header.VcfHeader;

/**
 * Only load records when asked for
 */
class TabixVcfRecordSet implements VariantSet {

  private final File mBaselineFile;
  private final File mCallsFile;
  private final String mSampleName;
  private final Collection<Pair<String, Integer>> mNames = new ArrayList<>();

  private final VcfHeader mBaseLineHeader;
  private final VcfHeader mCalledHeader;
  private final boolean mPassOnly;
  private final boolean mSquashPloidy;
  private final int mMaxLength;
  private final RocSortValueExtractor mExtractor;

  private Pair<String, Integer> mCurrentNameLength;
  private int mBaselineSkipped;
  private int mCallsSkipped;

  TabixVcfRecordSet(File baselineFile, File calledFile, String sampleName, RocSortValueExtractor extractor, boolean passOnly, boolean squashPloidy, Collection<Pair<String, Integer>> referenceNameOrdering, int maxLength) throws IOException {
    if (referenceNameOrdering == null) {
      throw new NullPointerException();
    }
    mBaselineFile = baselineFile;
    mCallsFile = calledFile;
    mSampleName = sampleName;
    mBaseLineHeader = VcfUtils.getHeader(baselineFile);
    mCalledHeader = VcfUtils.getHeader(calledFile);
    mPassOnly = passOnly;
    mSquashPloidy = squashPloidy;
    mExtractor = extractor;
    mMaxLength = maxLength;
    final TabixIndexReader baseTir = new TabixIndexReader(TabixIndexer.indexFileName(baselineFile));
    final Set<String> basenames = new TreeSet<>();
    Collections.addAll(basenames, baseTir.sequenceNames());
    final TabixIndexReader callTir = new TabixIndexReader(TabixIndexer.indexFileName(calledFile));
    final Set<String> callnames = new TreeSet<>();
    Collections.addAll(callnames, callTir.sequenceNames());
    final Set<String> referenceNames = new HashSet<>();
    for (Pair<String, Integer> orderedNameLength : referenceNameOrdering) {
      final String name = orderedNameLength.getA();
      referenceNames.add(name);
      if (basenames.contains(name)) {
        if (callnames.contains(name)) {
          mNames.add(orderedNameLength);
        } else {
          mNames.add(orderedNameLength);
          Diagnostic.warning("Reference sequence " + name + " is declared in baseline but not declared in calls (variants will be treated as FN).");
        }
      } else {
        if (callnames.contains(name)) {
          mNames.add(orderedNameLength);
          Diagnostic.warning("Reference sequence " + name + " is declared in calls but not declared in baseline (variants will be treated as FP).");
        } else {
          Diagnostic.userLog("Skipping reference sequence " + name + " that is used by neither baseline or calls.");
        }
      }
    }
    for (String name : basenames) {
      if (!referenceNames.contains(name)) {
        Diagnostic.warning("Baseline variants for sequence " + name + " will be ignored as this sequence is not contained in the reference.");
      }
    }
    for (String name : callnames) {
      if (!referenceNames.contains(name)) {
        Diagnostic.warning("Call set variants for sequence " + name + " will be ignored as this sequence is not contained in the reference.");
      }
    }
  }

  @Override
  public Map<VariantSetType, List<DetectedVariant>> nextSet() {
    final Map<VariantSetType, List<DetectedVariant>> map = new HashMap<>();
    final Iterator<Pair<String, Integer>> iterator = mNames.iterator();
    if (!iterator.hasNext()) {
      return null;
    }
    mCurrentNameLength = iterator.next();
    mNames.remove(mCurrentNameLength);
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      final FutureTask<LoadedVariants> baseFuture = new FutureTask<>(new VcfRecordTabixCallable(mBaselineFile, mSampleName, mCurrentNameLength, VariantSetType.BASELINE, mExtractor, mPassOnly, mSquashPloidy, mMaxLength));
      final FutureTask<LoadedVariants> callFuture = new FutureTask<>(new VcfRecordTabixCallable(mCallsFile, mSampleName, mCurrentNameLength, VariantSetType.CALLS, mExtractor, mPassOnly, mSquashPloidy, mMaxLength));
      executor.execute(baseFuture);
      executor.execute(callFuture);
      final LoadedVariants baseVars = baseFuture.get();
      final LoadedVariants calledVars = callFuture.get();
      map.put(VariantSetType.BASELINE, baseVars.mVariants);
      map.put(VariantSetType.CALLS, calledVars.mVariants);
      mBaselineSkipped += baseVars.mSkippedDuringLoading;
      mCallsSkipped += calledVars.mSkippedDuringLoading;
      Diagnostic.userLog("Reference " + mCurrentNameLength.getA() + " baseline contains " + map.get(VariantSetType.BASELINE).size() + " variants.");
      Diagnostic.userLog("Reference " + mCurrentNameLength.getA() + " calls contains " + map.get(VariantSetType.BASELINE).size() + " variants.");
    } catch (final ExecutionException e) {
      throw new NoTalkbackSlimException(e.getCause(), ErrorType.INFO_ERROR, e.getCause().getMessage());
    } catch (final InterruptedException e) {
      throw new NoTalkbackSlimException(e, ErrorType.INFO_ERROR, e.getCause().getMessage());
    } finally {
      executor.shutdownNow();
    }
    return map;
  }

  @Override
  public String currentName() {
    return mCurrentNameLength.getA();
  }

  @Override
  public VcfHeader baseLineHeader() {
    return mBaseLineHeader;
  }

  @Override
  public VcfHeader calledHeader() {
    return mCalledHeader;
  }

  @Override
  public int getNumberOfSkippedBaselineVariants() {
    return mBaselineSkipped;
  }

  @Override
  public int getNumberOfSkippedCalledVariants() {
    return mCallsSkipped;
  }

}

