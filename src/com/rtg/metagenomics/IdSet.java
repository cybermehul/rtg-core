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
package com.rtg.metagenomics;

import java.util.Arrays;

/**
 * Array of integers that will equal another array with the same contents.
 */
public class IdSet {

  private final int[] mIds;

  IdSet(final int[] ids) {
    mIds = ids;
  }

  @Override
  public boolean equals(final Object that) {
    return that instanceof IdSet && Arrays.equals(((IdSet) that).mIds, mIds);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(mIds);
  }

}
