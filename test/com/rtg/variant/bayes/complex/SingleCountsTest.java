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
package com.rtg.variant.bayes.complex;

import static com.rtg.util.StringUtils.TAB;

import java.io.IOException;

import junit.framework.TestCase;

/**
 */
public class SingleCountsTest extends TestCase {

  public void test() throws IOException {
    final SingleCounts sc = new SingleCounts();
    assertEquals(0, sc.count());
    check(sc, "0:0.000", TAB + "0" + TAB + "0.000");
    sc.increment(0.01);
    assertEquals(1, sc.count());
    check(sc, "1:0.010", TAB + "1" + TAB + "0.010");
  }

  private void check(final SingleCounts sc, final String toString,
      final String outString) {
    sc.integrity();
    assertEquals(toString, sc.toString());
    final String out = sc.output();
    assertEquals(outString, out);
  }
}
