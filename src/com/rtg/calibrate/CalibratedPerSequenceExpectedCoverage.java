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
package com.rtg.calibrate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.reeltwo.jumble.annotations.TestClass;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.RegionRestriction;

/**
 * Get the expected coverage per sequence and sample from calibration data.
 */
@TestClass("com.rtg.variant.CalibratedPerSequenceThresholdTest")
public class CalibratedPerSequenceExpectedCoverage {

  // For each sequence, stores per sample coverage
  final Map<String, Map<String, Double>> mSequenceSampleCoverages;
  // For each sequence, stores coverage using total coverage across all samples
  final Map<String, Double> mSumCoverages;
  final Set<String> mSamples;

  /**
   *
   * @param calibrator the mapping calibration stats to use when computing coverage
   * @param defaultSequenceLengths map of sequence names to sequence length, used if not already set in the calibrator
   * @param readGroupToSampleId read group to sample id
   * @param restriction a region restriction, may be null
   */
  public CalibratedPerSequenceExpectedCoverage(Calibrator calibrator, Map<String, Integer> defaultSequenceLengths, Map<String, String> readGroupToSampleId, RegionRestriction restriction) {
    mSequenceSampleCoverages = new HashMap<>();
    mSumCoverages = new HashMap<>();
    mSamples = Collections.unmodifiableSet(new HashSet<>(readGroupToSampleId.values()));
    final Map<String, Integer> sequenceLengths = calibrator.hasLengths() ? calibrator.getSequenceLengths() : defaultSequenceLengths;
    if (calibrator.getCovariateIndex(CovariateEnum.SEQUENCE) == -1) { // No per sequence separation in calibration data, calculate per-genome coverage level
      long length = 0;
      for (final Map.Entry<String, Integer> entry : sequenceLengths.entrySet()) {
        length += entry.getValue();
      }
      final Map<String, HashMap<String, CalibrationStats>> local = new HashMap<>();
      findIndividualGlobalCoverage(calibrator, readGroupToSampleId, local);
      double currentMax = 0;
      double currentSum = 0;
      for (Map.Entry<String, HashMap <String, CalibrationStats>> e : local.entrySet()) {
        final HashMap<String, CalibrationStats> map = e.getValue();
        if (map.containsKey(DUMMY_SEQ)) {
          final double currentCov = (length == 0) ? 0 : (double) map.get(DUMMY_SEQ).getTotalLength() / length;
          final String sampleName = e.getKey();
          Diagnostic.userLog("Average coverage for sample " + sampleName + " is " + Utils.realFormat(currentCov, 2));
          for (final Map.Entry<String, Integer> entry : sequenceLengths.entrySet()) {
            final String seqName = entry.getKey();
            Map<String, Double> samples = mSequenceSampleCoverages.get(seqName);
            if (samples == null) {
              samples = new HashMap<>();
              mSequenceSampleCoverages.put(seqName, samples);
            }
            samples.put(sampleName, currentCov);
          }
          currentSum += currentCov;
          if (currentCov > currentMax) {
            currentMax = currentCov;
          }
        }
      }
      Diagnostic.userLog("Average combined coverage is " + Utils.realFormat(currentSum, 2));
      for (final Map.Entry<String, Integer> entry : sequenceLengths.entrySet()) {
        final String seqName = entry.getKey();
        mSumCoverages.put(seqName, currentSum);
      }
    } else { // Per-sequence separation is in calibration data, calculate per-sequence coverage level
      final Map<String, HashMap<String, CalibrationStats>> local = new HashMap<>();
      findIndividualPerSequenceCoverage(calibrator, sequenceLengths, readGroupToSampleId, local, restriction);
      for (final Map.Entry<String, Integer> entry : sequenceLengths.entrySet()) {
        final String seqName = entry.getKey();
        if (restriction != null && !seqName.equals(restriction.getSequenceName())) {
          continue;
        }
        final int seqLength = entry.getValue();
        double currentMax = 0;
        double currentSum = 0;
        for (Map.Entry<String, HashMap <String, CalibrationStats>> e : local.entrySet()) {
          final HashMap<String, CalibrationStats> map = e.getValue();
          if (map.containsKey(seqName)) {
            final double currentCov = (seqLength == 0) ? 0 : (double) map.get(seqName).getTotalLength() / seqLength;
            final String sampleName = e.getKey();
            Diagnostic.userLog("Average coverage across sequence " + seqName + " for sample " + sampleName + " is " + Utils.realFormat(currentCov, 2));
            Map<String, Double> samples = mSequenceSampleCoverages.get(seqName);
            if (samples == null) {
              samples = new HashMap<>();
              mSequenceSampleCoverages.put(seqName, samples);
            }
            samples.put(sampleName, currentCov);
            currentSum += currentCov;
            if (currentCov > currentMax) {
              currentMax = currentCov;
            }
          }
        }
        Diagnostic.userLog("Average combined coverage for sequence " + seqName + " is " + Utils.realFormat(currentSum, 2));
        mSumCoverages.put(seqName, currentSum);
      }
    }
  }

  /** Used as a dummy sequence name when accumulating genome-wide stats */
  private static final String DUMMY_SEQ = "dummy";

  private static void findIndividualGlobalCoverage(Calibrator calibrator, Map<String, String> readGroupToSampleId, final Map<String, HashMap<String, CalibrationStats>> local) {
    final Covariate rgCovariate = calibrator.getCovariate(calibrator.getCovariateIndex(CovariateEnum.READGROUP));
    for (final Map.Entry<String, String> e2 : readGroupToSampleId.entrySet()) {
      final String readGroup = e2.getKey();
      final String sampleName = e2.getValue();
      final int rgValue = rgCovariate.valueOf(readGroup);
      if (rgValue == -1) {
        add(local, sampleName, DUMMY_SEQ, new CalibrationStats(null));
      } else {
        final Calibrator.QuerySpec spec = calibrator.initQuery();
        spec.setValue(CovariateEnum.READGROUP, rgValue);
        calibrator.processStats(new LocalStatsProcessor(local, sampleName, DUMMY_SEQ), spec);
      }
    }
  }

  private static void findIndividualPerSequenceCoverage(Calibrator calibrator, Map<String, Integer> sequenceLengths, Map<String, String> readGroupToSampleId, final Map<String, HashMap<String, CalibrationStats>> local, RegionRestriction restriction) {
    final Covariate rgCovariate = calibrator.getCovariate(calibrator.getCovariateIndex(CovariateEnum.READGROUP));
    final Covariate seqCovariate = calibrator.getCovariate(calibrator.getCovariateIndex(CovariateEnum.SEQUENCE));
    for (final Map.Entry<String, Integer> entry : sequenceLengths.entrySet()) {
      final String sequenceName = entry.getKey();
      if (restriction != null && !sequenceName.equals(restriction.getSequenceName())) {
        continue;
      }
      for (final Map.Entry<String, String> e2 : readGroupToSampleId.entrySet()) {
        final String readGroup = e2.getKey();
        final String sampleName = e2.getValue();
        final int rgValue = rgCovariate.valueOf(readGroup);
        final int seqValue = seqCovariate.valueOf(sequenceName);
        if (rgValue == -1 || seqValue == -1) {
          add(local, sampleName, sequenceName, new CalibrationStats(null));
        } else {
          final Calibrator.QuerySpec spec = calibrator.initQuery();
          spec.setValue(CovariateEnum.READGROUP, rgValue);
          spec.setValue(CovariateEnum.SEQUENCE, seqValue);
          calibrator.processStats(new LocalStatsProcessor(local, sampleName, sequenceName), spec);
        }
      }
    }
  }

  private static final class LocalStatsProcessor implements StatsProcessor {

    private final Map<String, HashMap<String, CalibrationStats>> mLocal;
    private final String mSampleName;
    private final String mSequenceName;

    private LocalStatsProcessor(Map<String, HashMap<String, CalibrationStats>> local, String sampleName, String sequenceName) {
      mLocal = local;
      mSampleName = sampleName;
      mSequenceName = sequenceName;
    }

    @Override
    public void process(int[] covariateValues, CalibrationStats stats) {
      add(mLocal, mSampleName, mSequenceName, stats);
    }

  }

  private static void add(Map<String, HashMap<String, CalibrationStats>> thresholds, String sampleId, String sequenceName, CalibrationStats covariateValues) {
    final HashMap<String, CalibrationStats> map;
    if (thresholds.containsKey(sampleId)) {
      map = thresholds.get(sampleId);
    } else {
      map = new HashMap<>();
      thresholds.put(sampleId, map);
    }

    if (map.containsKey(sequenceName)) {
      if (covariateValues != null) {
        map.get(sequenceName).accumulate(covariateValues);
      }
    } else {
      final CalibrationStats stats = new CalibrationStats(null);
      if (covariateValues != null) {
        stats.accumulate(covariateValues);
      }
      map.put(sequenceName, stats);
    }
  }

  /**
   * @param sampleName the sample name
   * @return true if we have calibrated coverage information for this sample
   */
  public boolean containsSample(String sampleName) {
    return mSamples.contains(sampleName);
  }

  /**
   * @return the set of samples that calibration information is available for
   */
  public Set<String> samples() {
    return mSamples;
  }

  /**
   * @return the set of sequences that calibration information if available for
   */
  public Collection<String> sequences() {
    return mSumCoverages.keySet();
  }

  /**
   * Get the expected coverage for the given sequence and sample names
   * @param sequenceName the sequence name
   * @param sampleName the sample name
   * @return the expected coverage for the given sequence and sample names, or -1 if not known
   */
  public double expectedCoverage(String sequenceName, String sampleName) {
    final Map<String, Double> samples = mSequenceSampleCoverages.get(sequenceName);
    if (samples == null) {
      throw new NoTalkbackSlimException("Unknown sequence: " + sequenceName);
    }
    if (!samples.containsKey(sampleName)) {
      //imputed sample ?
      return -1;
      //throw new NoTalkbackSlimException("Unknown sample: " + sampleName);
    }
    return samples.get(sampleName);
  }

  /**
   * Get the expected coverage for the given sequence name over all samples
   * @param sequenceName the sequence name
   * @return the expected coverage for the given sequence name over all samples, or -1 if not known
   */
  public double expectedTotalCoverage(String sequenceName) {
    if (!mSumCoverages.containsKey(sequenceName)) {
      throw new NoTalkbackSlimException("Unknown sequence: " + sequenceName);
    }
    return mSumCoverages.get(sequenceName);
  }

}
