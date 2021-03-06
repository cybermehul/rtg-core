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
package com.rtg.util.diagnostic;


import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the corresponding class.
 *
 */
public class WarningEventTest extends AbstractDiagnosticEventTest {

  /**
   */
  public WarningEventTest(final String name) {
    super(name);
  }
  public static Test suite() {
    return new TestSuite(WarningEventTest.class);
  }
  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  @Override
  public DiagnosticEvent<?> getEvent() {
    return new WarningEvent(WarningType.NO_NAME, "no-name-sequence");
  }

  @Override
  public Class<?> getEnumClass() {
    return WarningType.class;
  }

}

