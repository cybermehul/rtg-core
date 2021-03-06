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

package com.rtg.variant.sv.discord.pattern;

import com.rtg.vcf.VcfRecord;

import junit.framework.TestCase;

/**
 *         Date: 16/03/12
 *         Time: 10:46 AM
 */
public class VcfBreakpointTest extends TestCase {
  public void test() {
    VcfBreakpoint breakpoint = makeBreakpoint("asdf", 100, "A[chr1:300[");
    assertEquals("VcfBreakpoint: asdf 100 chr1 300 true false", breakpoint.toString());
    assertEquals("asdf", breakpoint.getLocalChr());
    assertEquals(100, breakpoint.getLocalPos());
    assertEquals("chr1", breakpoint.getRemoteChr());
    assertEquals(300, breakpoint.getRemotePos());
    assertEquals(0, breakpoint.getDepth());
    assertTrue(breakpoint.isLocalUp());
    assertFalse(breakpoint.isRemoteUp());
    VcfBreakpoint breakpoint2 = makeBreakpoint("asdf", 100, "A[chr1:299[");
    assertEquals(0, breakpoint.compareTo(breakpoint));
    assertEquals(1, breakpoint.compareTo(breakpoint2));
    assertEquals(-1, breakpoint2.compareTo(breakpoint));
    breakpoint2 = makeBreakpoint("asde", 100, "A[chr1:300[");
    assertEquals(1, breakpoint.compareTo(breakpoint2));
    breakpoint2 = makeBreakpoint("asdf", 99, "A[chr1:300[");
    assertEquals(1, breakpoint.compareTo(breakpoint2));
    breakpoint2 = makeBreakpoint("asdf", 100, "A[chm:300[");
    assertEquals("chr1".compareTo("chm"), breakpoint.compareTo(breakpoint2));
    breakpoint2 = makeBreakpoint("asdf", 100, "[chr1:300[A");
    assertEquals(1, breakpoint.compareTo(breakpoint2));
    breakpoint2 = makeBreakpoint("asdf", 100, "A]chr1:300]");
    assertEquals(-1, breakpoint.compareTo(breakpoint2));
  }
  private static VcfBreakpoint makeBreakpoint(String local, int pos, String alt) {
    VcfRecord rec = makeRecord(local, pos, alt);
    return new VcfBreakpoint(rec);
  }

  private static VcfRecord makeRecord(String local, int pos, String alt) {
    VcfRecord bar = new VcfRecord();
    bar.setSequence(local);
    bar.setStart(pos - 1);
    bar.addAltCall(alt);
    return bar;
  }

  public void testDepth() {
    VcfRecord rec = makeRecord("foo", 20, "A[chr:200[");
    rec.addInfo("DP", "5");
    rec.addInfo("DP", "2");
    VcfBreakpoint breakpoint = new VcfBreakpoint(rec);
    assertEquals(7, breakpoint.getDepth());

  }

  static final VcfBreakpoint BREAK_POINT = makeBreakpoint("foo", 20, "A[chr1:200[");
  public void testEquals() {
    VcfBreakpoint b2 = makeBreakpoint("foo", 21, "A[chr1:200[");
    assertFalse(BREAK_POINT.equals(null));
    assertFalse(BREAK_POINT.equals("monkey"));
    assertTrue(BREAK_POINT.equals(BREAK_POINT));
    assertFalse(BREAK_POINT.equals(b2));
  }

  public void testHashCode() {
    assertEquals(-734628597, BREAK_POINT.hashCode());
  }
}
