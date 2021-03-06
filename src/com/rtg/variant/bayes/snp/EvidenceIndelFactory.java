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

package com.rtg.variant.bayes.snp;

import com.rtg.launcher.GlobalFlags;
import com.rtg.variant.bayes.EvidenceInterface;
import com.rtg.variant.bayes.Model;

/**
 */
public final class EvidenceIndelFactory implements CachedEvidenceFactory {

  //TODO cache these if important
  private static final boolean COMPLEX_REGION_INDEL_EXTENSION = GlobalFlags.getBooleanValue(GlobalFlags.COMPLEX_REGION_INDEL_EXTENSION);

  /** Unique instance of factory. */
  public static final CachedEvidenceFactory SINGLETON = new EvidenceIndelFactory();

  private EvidenceIndelFactory() {
  }

  // For efficiency precompute the possible evidence objects
  private final EvidenceIndel[] mGoodEvidence = {new EvidenceIndel(0, 0, 0), new EvidenceIndel(0, 1, 0)};
  private final EvidenceIndel[] mBadEvidence = {new EvidenceIndel(Model.AMBIGUITY_THRESHOLD, 0, 0), new EvidenceIndel(Model.AMBIGUITY_THRESHOLD, 1, 0)};

  @Override
  public EvidenceInterface evidence(int readNt, int readBasesLeft, int readBasesRight, int mapQ, int phred, int stateIndex, int maxIndelLength, boolean isUnmapped) {
    assert !isUnmapped; // Unmapped evidence should never be coming as indels
    if (mapQ <= Model.AMBIGUITY_PHRED) {
      return COMPLEX_REGION_INDEL_EXTENSION ? new EvidenceIndel(Model.AMBIGUITY_THRESHOLD, readNt, maxIndelLength) : mBadEvidence[readNt];
    } else {
      return COMPLEX_REGION_INDEL_EXTENSION ? new EvidenceIndel(0, readNt, maxIndelLength) : mGoodEvidence[readNt];
    }
  }

  // This one does not care about any of these states.
  @Override
  public int getStateIndex(boolean isForward, boolean isReadPaired, boolean isMated) {
    return 0;
  }
}
