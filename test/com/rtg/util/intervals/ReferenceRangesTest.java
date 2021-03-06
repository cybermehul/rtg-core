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

package com.rtg.util.intervals;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 */
public class ReferenceRangesTest extends TestCase {

  public void testRanges() {
    ReferenceRanges.Accumulator acc = new ReferenceRanges.Accumulator();
    for (int i = 0; i < 1000; i++) {
      acc.addRangeData("sequence1", new RangeList.RangeData<String>(i * 100, i * 100 + 50, "region" + i));
    }

    ReferenceRanges ranges = acc.getReferenceRanges();
    assertTrue(ranges.containsSequence("sequence1"));
    assertFalse(ranges.containsSequence("sequence2"));
    assertTrue(ranges.get("sequence1").getRangeList().size() == 1000);
    assertTrue(ranges.get("sequence1").getFullRangeList().size() == 2001);

    Map<String, Integer> ids = new HashMap<>();
    ids.put("sequence1", 42);
    ranges.setIdMap(ids);
    
    assertTrue(ranges.containsSequence(42));
    assertFalse(ranges.containsSequence(12));
    assertTrue(ranges.get(42).getRangeList().size() == 1000);


  }
}
