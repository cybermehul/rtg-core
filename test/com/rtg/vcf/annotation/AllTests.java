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
package com.rtg.vcf.annotation;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all tests in this directory.
 *
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    final TestSuite suite = new TestSuite("com.rtg.vcf.annotation");

    //$JUnit-BEGIN$
    suite.addTestSuite(AlleleCountInGenotypesAnnotationTest.class);
    suite.addTestSuite(AnnotationDataTypeTest.class);
    suite.addTestSuite(DerivedAnnotationsTest.class);
    suite.addTestSuite(DummyDerivedAnnotationTest.class);
    suite.addTestSuite(DummyDerivedFormatAnnotationTest.class);
    suite.addTestSuite(EquilibriumProbabilityAnnotationTest.class);
    suite.addTestSuite(GenotypeQualityOverDepthAnnotationTest.class);
    suite.addTestSuite(InbreedingCoefficientAnnotationTest.class);
    suite.addTestSuite(LongestAlleleAnnotationTest.class);
    suite.addTestSuite(NumberAllelesInGenotypesAnnotationTest.class);
    suite.addTestSuite(NumberOfAltAllelesAnnotationTest.class);
    suite.addTestSuite(PloidyAnnotationTest.class);
    suite.addTestSuite(QualOverDepthAnnotationTest.class);
    suite.addTestSuite(ZygosityAnnotationTest.class);
    //$JUnit-END$

    return suite;
  }

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
