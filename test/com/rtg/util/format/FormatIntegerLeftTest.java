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
package com.rtg.util.format;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test class for FormatIntegerLeft. <p>
 */
public class FormatIntegerLeftTest extends TestCase {

  /**
   */
  public FormatIntegerLeftTest(final String s) {
    super(s);
  }


  static final int[] TST_DATA =
    new int[]{0, 1, -1, 123, -123, Integer.MAX_VALUE, Integer.MIN_VALUE};


  public static void tst(final FormatIntegerLeft fr) {
    // do all the tests in one cycle - instead of calling the method - juri
    for (final int x : TST_DATA) {
      fr.format(x);
      //test that can put into exisiting string
      final StringBuilder sb = new StringBuilder("ABC");
      fr.format(sb, x);
    }
  }


  static final int[] LENGTHS = new int[]{0, 1, 2, 3, 4, 10, 12};


  public void testFormatIntegerLeft() {
    for (final int l : LENGTHS) {
      tst(new FormatIntegerLeft(l));
      tst(new FormatIntegerLeft(l, false));
      tst(new FormatIntegerLeft(l, true));
    }
  }


  public static void tstex(final int len, final boolean group, final long val, final String res) {
    final FormatIntegerLeft fr = new FormatIntegerLeft(len, group);
    final String s = fr.format(val);
    assertEquals(res, s);
  }


  public void testExamples() {
    tstex(0, false, 0, "0");
    tstex(1, false, 0, "0");
    tstex(2, false, 0, "0 ");
    tstex(0, false, 123, "123");
    tstex(1, false, 123, "123");
    tstex(3, false, 123, "123");
    tstex(4, false, 123, "123 ");
    tstex(0, false, 1234, "1234");
    tstex(1, false, 1234, "1234");
    tstex(3, false, 1234, "1234");
    tstex(4, false, 1234, "1234");
    tstex(5, false, 1234, "1234 ");
    tstex(6, false, 1234567, "1234567");
    tstex(7, false, 1234567, "1234567");
    tstex(8, false, 1234567, "1234567 ");
    tstex(9, false, 1234567890, "1234567890");
    tstex(10, false, 1234567890, "1234567890");
    tstex(11, false, 1234567890, "1234567890 ");
    tstex(9, false, 8234567890L, "8234567890");
    tstex(10, false, 8234567890L, "8234567890");
    tstex(11, false, 8234567890L, "8234567890 ");
    tstex(12, false, 8234567890123L, "8234567890123");
    tstex(13, false, 8234567890123L, "8234567890123");
    tstex(14, false, 8234567890123L, "8234567890123 ");

    tstex(0, true, -123, "-123");
    tstex(3, true, -123, "-123");
    tstex(4, true, -123, "-123");
    tstex(5, true, -123, "-123 ");
    tstex(4, true, -123, "-123");
    tstex(0, true, -1234, "-1,234");
    tstex(1, true, -1234, "-1,234");
    tstex(3, true, -1234, "-1,234");
    tstex(5, true, -1234, "-1,234");
    tstex(6, true, -1234, "-1,234");
    tstex(7, true, -1234, "-1,234 ");
    tstex(9, true, -1234567, "-1,234,567");
    tstex(10, true, -1234567, "-1,234,567");
    tstex(11, true, -1234567, "-1,234,567 ");
    tstex(13, true, -1234567890, "-1,234,567,890");
    tstex(14, true, -1234567890, "-1,234,567,890");
    tstex(15, true, -1234567890, "-1,234,567,890 ");
    tstex(13, true, -8234567890L, "-8,234,567,890");
    tstex(14, true, -8234567890L, "-8,234,567,890");
    tstex(15, true, -8234567890L, "-8,234,567,890 ");
    tstex(17, true, -8234567890123L, "-8,234,567,890,123");
    tstex(18, true, -8234567890123L, "-8,234,567,890,123");
    tstex(19, true, -8234567890123L, "-8,234,567,890,123 ");

    tstex(0, false, 123, "123");
    tstex(1, false, 123, "123");
    tstex(3, false, 123, "123");
    tstex(4, false, 123, "123 ");
    tstex(0, false, 1234, "1234");
    tstex(1, false, 1234, "1234");
    tstex(3, false, 1234, "1234");
    tstex(4, false, 1234, "1234");
    tstex(5, false, 1234, "1234 ");
    tstex(6, false, 1234567, "1234567");
    tstex(7, false, 1234567, "1234567");
    tstex(8, false, 1234567, "1234567 ");
    tstex(9, false, 1234567890, "1234567890");
    tstex(10, false, 1234567890, "1234567890");
    tstex(11, false, 1234567890, "1234567890 ");
    tstex(9, false, 8234567890L, "8234567890");
    tstex(10, false, 8234567890L, "8234567890");
    tstex(11, false, 8234567890L, "8234567890 ");
    tstex(12, false, 8234567890123L, "8234567890123");
    tstex(13, false, 8234567890123L, "8234567890123");
    tstex(14, false, 8234567890123L, "8234567890123 ");

    tstex(0, true, 0, "0");
    tstex(1, true, 0, "0");
    tstex(2, true, 0, "0 ");
    tstex(0, true, 123, "123");
    tstex(1, true, 123, "123");
    tstex(3, true, 123, "123");
    tstex(4, true, 123, "123 ");
    tstex(0, true, 1234, "1,234");
    tstex(1, true, 1234, "1,234");
    tstex(3, true, 1234, "1,234");
    tstex(4, true, 1234, "1,234");
    tstex(5, true, 1234, "1,234");
    tstex(6, true, 1234, "1,234 ");
    tstex(8, true, 1234567, "1,234,567");
    tstex(9, true, 1234567, "1,234,567");
    tstex(10, true, 1234567, "1,234,567 ");
    tstex(12, true, 1234567890, "1,234,567,890");
    tstex(13, true, 1234567890, "1,234,567,890");
    tstex(14, true, 1234567890, "1,234,567,890 ");
    tstex(12, true, 8234567890L, "8,234,567,890");
    tstex(13, true, 8234567890L, "8,234,567,890");
    tstex(14, true, 8234567890L, "8,234,567,890 ");
    tstex(16, true, 8234567890123L, "8,234,567,890,123");
    tstex(17, true, 8234567890123L, "8,234,567,890,123");
    tstex(18, true, 8234567890123L, "8,234,567,890,123 ");
  }


  public void testFormatIntegerLeftMIN() {
    try {
      new FormatIntegerLeft(Integer.MIN_VALUE);
      fail("should throw new IllegalArgumentException");
    } catch (final IllegalArgumentException e) { }
  }

  public void testToString() {
    FormatIntegerLeft left = new FormatIntegerLeft(12);

    StringBuilder buff = new StringBuilder(11);
    buff = left.format(buff, 54555L);

    assertEquals("54555       ", buff.toString());
  }


  public void testFormatIntegerLeftNegative() {

    try {
      new FormatIntegerLeft(-100);
      fail("should throw IllegalArgumentException");
    } catch (final IllegalArgumentException e) { }

  }


  public void testFormatNull() {

    final FormatIntegerLeft fl = new FormatIntegerLeft(10);
    try {
      fl.format(null, 10);
      fail("should throw nullPointerException");
    } catch (final NullPointerException e) { }
  }


  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new TestSuite(FormatIntegerLeftTest.class));
    return suite;
  }


  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

}


