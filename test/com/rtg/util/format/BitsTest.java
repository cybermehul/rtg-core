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
 *
 *
 */
public class BitsTest extends TestCase {

  /**
   */
  public BitsTest(final String name) {
    super(name);
  }

  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new TestSuite(BitsTest.class));
    return suite;
  }

  public void test() {
    final Bits b = new Bits(5.5f);

    assertEquals(12, b.maxLength());

    assertEquals("         5.5", b.toString());
  }

}
