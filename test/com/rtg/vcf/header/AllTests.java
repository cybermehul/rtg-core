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
package com.rtg.vcf.header;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all tests in this directory.
 *
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    final TestSuite suite = new TestSuite("com.rtg.vcf");

    //$JUnit-BEGIN$
    suite.addTestSuite(ContigFieldTest.class);
    suite.addTestSuite(AltFieldTest.class);
    suite.addTestSuite(FilterFieldTest.class);
    suite.addTestSuite(FormatFieldTest.class);
    suite.addTestSuite(InfoFieldTest.class);
    suite.addTestSuite(MetaTypeTest.class);
    suite.addTestSuite(MetaTypeTest.class);
    suite.addTestSuite(SampleFieldTest.class);
    suite.addTestSuite(PedigreeFieldTest.class);
    suite.addTestSuite(VcfHeaderTest.class);
    suite.addTestSuite(VcfHeaderMergeTest.class);
    suite.addTestSuite(VcfNumberTest.class);
    suite.addTestSuite(VcfNumberTypeTest.class);
    //$JUnit-END$

    return suite;
  }

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
