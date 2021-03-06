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

package com.rtg.variant.sv;

import com.rtg.util.TestUtils;
import com.rtg.util.io.MemoryPrintStream;

import net.sf.samtools.SAMRecord;

import junit.framework.TestCase;

/**
 */
public class SignalSumTest extends TestCase {

  public void test() {
    final SamCounts sa = new SamArray(10);
    final SAMRecord sam1 = new SAMRecord(null);
    sam1.setReadName("1");
    final SAMRecord sam2 = new SAMRecord(null);
    sam2.setReadName("2");
    final SAMRecord sam3 = new SAMRecord(null);
    sam3.setReadName("3");
    sa.increment(0);
    sa.increment(4);
    sa.increment(4);
    final Signal sigc = new SignalCount(sa, -3, 3, "");
    try (MemoryPrintStream ps = new MemoryPrintStream()) {
      final Signal sig = new SignalSum(ps.printStream(), "blah", sigc, sigc);
      assertEquals("Sum:", sig.toString());
      assertEquals(1.0 * 2, sig.value(0));
      assertEquals(1.0 * 2, sig.value(1));
      assertEquals(3.0 * 2, sig.value(2));
      assertEquals(3.0 * 2, sig.value(3));
      assertEquals(2.0 * 2, sig.value(4));
      assertEquals(2.0 * 2, sig.value(5));
      assertEquals(2.0 * 2, sig.value(6));
      assertEquals(2.0 * 2, sig.value(7));
      assertEquals(0.0 * 2, sig.value(8));
      assertEquals(0.0 * 2, sig.value(9));
      assertEquals(0.0 * 2, sig.value(10));
      assertEquals("blah", sig.columnLabel());
      TestUtils.containsAll(ps.toString().replace("\t", "  "), "0  1.00  1.00  2.00", "10  0.00  0.00  0.00");
    }
  }

  public void test0() {
    final Signal sig = new SignalSum("blah");
    assertEquals(0.0, sig.value(0));
    assertEquals("blah", sig.columnLabel());
  }

}
