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

/**
 * Information about a mates position.
 */
public interface MateInfo {

  /**
   * @return true if record was mated during alignment
   */
  boolean isMated();

  /**
   * @return the start position of the mates alignment (zero based)
   */
  int getFragmentLength();

  /**
   * @return the index of the mates corresponding reference sequence
   */
  int getMateSequenceId();
}
