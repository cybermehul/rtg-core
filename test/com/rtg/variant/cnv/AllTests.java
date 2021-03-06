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
package com.rtg.variant.cnv;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all tests in this package.
 *
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    final TestSuite suite = new TestSuite("com.rtg.variant.cnv");

    suite.addTest(com.rtg.variant.cnv.region.AllTests.suite());

    suite.addTestSuite(CnvStatsCliTest.class);
    suite.addTestSuite(CnvStatsTest.class);
    suite.addTestSuite(CnvProductParamsTest.class);
    suite.addTestSuite(CnvProductTaskTest.class);
    suite.addTestSuite(CnvProductCliTest.class);
    suite.addTestSuite(CnvRatioTest.class);
    suite.addTestSuite(SimpleOutputTest.class);
    suite.addTestSuite(GnuOutputTest.class);
    suite.addTestSuite(LeastSquaresModelTest.class);
    suite.addTestSuite(NBlockDetectorTest.class);
    return suite;
  }

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
