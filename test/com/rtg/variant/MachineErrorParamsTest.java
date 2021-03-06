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
package com.rtg.variant;

import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.StringUtils;

import junit.framework.TestCase;

/**
 */
public class MachineErrorParamsTest extends TestCase {

  public void testGlobalIntegrity() throws InvalidParamsException, IOException {
    final MachineErrorParams params = MachineErrorParams.builder()
    .errors("testsequencer_errors")
    .create();
    assertTrue(params.globalIntegrity());
  }

  /** Check that Illumina error priors parse okay. */
  public void testIllumina() throws InvalidParamsException, IOException {
    final MachineErrorParams params = MachineErrorParams.builder()
    .errors("illumina")
    .create();
    assertEquals(0.0092188, params.errorSnpRate(), 0.00001);
    assertEquals(0.000058, params.errorInsEventRate());
    assertEquals(0.000083, params.errorDelEventRate());
    assertEquals(0, params.qualityCurve()[0]);
    assertEquals(1, params.qualityCurve()[1]);
    assertEquals(63, params.qualityCurve()[63]);
    assertEquals(false, params.isCG());
    assertNotNull(params.realignParams());
  }

  public void testPhred() throws InvalidParamsException, IOException {
    final MachineErrorParams params = MachineErrorParams.builder()
      .errors("illumina").create();

    assertEquals(62, params.getPhred((char) ('!' + 62), 1));
    assertEquals(63, params.getPhred((char) ('!' + 63), 1));
    assertEquals(63, params.getPhred((char) ('!' + 64), 1));
  }

  /** Check that CG error priors parse okay. */
  public void testCG() throws InvalidParamsException, IOException {
    final AbstractMachineErrorParams params = MachineErrorParams.builder()
    .errors("complete")
    .create();
    assertEquals(0.03618, params.errorSnpRate());
    assertEquals(0.0020, params.errorInsEventRate());
    assertEquals(0.0010, params.errorDelEventRate());

    // test the quality curve
    final int[] qcurve = params.qualityCurve();
    assertNotNull(qcurve);
    assertEquals(64, qcurve.length);
    assertEquals(0, qcurve[0]);
    assertEquals(10, qcurve[9]);
    assertEquals(10, qcurve[29]);
    assertEquals(10, qcurve[63]);
    assertEquals(true, params.cgTrimOuterBases());
    assertEquals(true, params.isCG());
  }

  /** Check that 454 error priors parse okay. */
  public void test454se() throws InvalidParamsException, IOException {
    final AbstractMachineErrorParams params = MachineErrorParams.builder()
    .errors("ls454_se")
    .create();
    assertEquals(0.03156, params.errorSnpRate(), 0.0001);
    assertEquals(0.0039, params.errorInsEventRate());
    assertEquals(0.0018, params.errorDelEventRate());
    assertEquals(14, params.qualityCurve()[25]);
    assertEquals(false, params.isCG());
  }

  /** Check that 454 error priors parse okay. */
  public void test454pe() throws InvalidParamsException, IOException {
    final AbstractMachineErrorParams params = MachineErrorParams.builder()
    .errors("ls454_pe")
    .create();
    assertEquals(0.00461, params.errorSnpRate(), 0.0001);
    assertEquals(0.0012, params.errorInsEventRate());
    assertEquals(0.0011, params.errorDelEventRate());
    assertEquals(21, params.qualityCurve()[25]);
    assertEquals(false, params.isCG());
  }

  private void checkExpected(final AbstractMachineErrorParams params) {
    assertEquals(0.010, params.errorMnpEventRate());
    assertEquals(0.019, params.errorSnpRate());
    final double[] mnpErrDist = params.errorMnpDistribution();
    assertEquals(5, mnpErrDist.length);
    assertEquals(0.00, mnpErrDist[0]);
    assertEquals(0.50, mnpErrDist[1]);
    assertEquals(0.20, mnpErrDist[2]);
    assertEquals(0.20, mnpErrDist[3]);
    assertEquals(0.10, mnpErrDist[4]);
    assertEquals(0.0098, params.errorInsBaseRate(), 0.000001);
    assertEquals(0.007, params.errorInsEventRate());
    assertEquals(0.0042, params.errorDelBaseRate(), 0.000001);
    assertEquals(0.003, params.errorDelEventRate());
    final double[] insErrDist = params.errorInsDistribution();
    assertEquals(5, insErrDist.length);
    assertEquals(0.00, insErrDist[0]);
    assertEquals(0.75, insErrDist[1]);
    assertEquals(0.15, insErrDist[2]);
    assertEquals(0.05, insErrDist[3]);
    assertEquals(0.05, insErrDist[4]);
    final double[] delErrDist = params.errorDelDistribution();
    assertEquals(4, delErrDist.length);
    assertEquals(0.00, delErrDist[0]);
    assertEquals(0.70, delErrDist[1]);
    assertEquals(0.20, delErrDist[2]);
    assertEquals(0.10, delErrDist[3]);
    assertNull(params.qualityCurve());
    assertEquals(false, params.cgTrimOuterBases());
    final double[] overlapDist = params.overlapDistribution();
    assertNotNull(overlapDist);
    assertEquals(5, overlapDist.length);
    assertEquals(0, overlapDist[1], 1e-8);
    assertEquals(0, overlapDist[2], 1e-8);
    assertEquals(1, overlapDist[3], 1e-8);
    final double[] gapDist = params.gapDistribution();
    assertNotNull(gapDist);
    assertEquals(5, gapDist.length);
    assertEquals(0, gapDist[1], 1e-8);
    assertEquals(1, gapDist[2], 1e-8);
    assertEquals(0, gapDist[3], 1e-8);
  }

  public void testDefaults() throws InvalidParamsException, IOException {
    final AbstractMachineErrorParams params = MachineErrorParams.builder()
    .errors("testsequencer_errors")
    .create();
    checkExpected(params);
    checkExpected(new MachineErrorParamsBuilder(params).create());
  }

  public void testBuilder() {
    final MachineErrorParamsBuilder builder = MachineErrorParams.builder();
    assertNotNull(builder);
    assertTrue(builder == builder.errorDelEventRate(0.0098));
    assertTrue(builder == builder.errorInsEventRate(0.095));
    assertTrue(builder == builder.errorInsDistribution(new double[] {0.23, 0.24, 0.53}));
    assertTrue(builder == builder.errorDelDistribution(new double[] {0.90, 0.10}));

    final AbstractMachineErrorParams params = builder.create();
    assertEquals(0.095, params.errorInsEventRate());
    assertEquals(0.0098, params.errorDelEventRate());
    final double[] insErrDist = params.errorInsDistribution();
    assertEquals(4, insErrDist.length);
    assertEquals(0.00, insErrDist[0]);
    assertEquals(0.23, insErrDist[1]);
    assertEquals(0.24, insErrDist[2]);
    assertEquals(0.53, insErrDist[3]);
    final double[] delErrDist = params.errorDelDistribution();
    assertEquals(3, delErrDist.length);
    assertEquals(0.00, delErrDist[0]);
    assertEquals(0.90, delErrDist[1]);
    assertEquals(0.10, delErrDist[2]);
  }

  public void testBadRate() {
    try {
      MachineErrorParams.builder().errorDelEventRate(2.0).create();
      fail("expected bad probability exception");
    } catch (final IllegalArgumentException e) {
      assertEquals("rate must be 0.0 .. 1.0, not 2.000000", e.getMessage());
    }
  }

  public void testToString() throws InvalidParamsException, IOException {
    final AbstractMachineErrorParams params = MachineErrorParams.builder()
    .errors("testsequencer_errors")
    .create();
    assertEquals("     insert errors=0.0098000 delete errors=0.0042000" + StringUtils.LS,
        params.toString());
  }

  public void testClosedMethod() throws InvalidParamsException, IOException {
    assertFalse(MachineErrorParams.builder()
    .errors("testsequencer_errors")
    .create().closed());
  }
}
