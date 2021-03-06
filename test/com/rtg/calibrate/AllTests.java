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
package com.rtg.calibrate;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests.
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    final TestSuite suite = new TestSuite("com.rtg.alignment");
    suite.addTestSuite(CalibrationStatsTest.class);
    suite.addTestSuite(CalibratorTest.class);
    suite.addTestSuite(CovariateBaseQualityTest.class);
    suite.addTestSuite(CovariateEnumTest.class);
    suite.addTestSuite(CovariateReadPosTest.class);
    suite.addTestSuite(CovariateReadGroupTest.class);
    suite.addTestSuite(CovariateSequenceTest.class);
    suite.addTestSuite(CovariateSequenceFixedTest.class);
    suite.addTestSuite(CovariateSingleReadGroupTest.class);
    suite.addTestSuite(ChrStatsTest.class);
    suite.addTestSuite(ChrStatsCliTest.class);
    suite.addTestSuite(RecalibrateCliTest.class);
    suite.addTestSuite(RecalibratingPopulatorFactoryTest.class);
    suite.addTestSuite(SamCalibrationInputsTest.class);
    return suite;
  }

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}

