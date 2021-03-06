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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.rtg.launcher.OutputParams;
import com.rtg.util.TestUtils;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.params.TestParams;
import com.rtg.variant.cnv.CnvProductParams.CnvProductParamsBuilder;

import junit.framework.TestCase;

/**
 * Test class
 */
public class CnvProductParamsTest extends TestCase {

  /** Because we want it to be hard to be agile! yay */
  public void testDefaults() throws IOException {
    final CnvProductParams.CnvProductParamsBuilder builder = CnvProductParams.builder();
    assertNotNull(builder);
    final CnvProductParams params = builder.create();
    assertNotNull(params);
    assertEquals(100, params.bucketSize());
    assertTrue(params.closed());

    assertNull(params.directory());

    try {
      params.file("file");
      fail();
    } catch (final RuntimeException e) {
      //ignore
    }
    assertEquals(null, params.outputParams());
    assertEquals(true, params.filterStartPositions());
    assertEquals(null, params.mappedBase());
    assertEquals(null, params.mappedTarget());
    assertEquals(-1, params.filterParams().maxAlignmentCount());
    assertEquals(null, params.filterParams().maxMatedAlignmentScore());
    assertEquals(null, params.filterParams().maxUnmatedAlignmentScore());
    assertEquals(3.0, params.divisionFactor());
    assertEquals(3.0, params.multiplicationFactor());
  }

  public void testActual() throws IOException {
    final File tempFile = FileUtils.createTempDir("cnvproductparams", "test");
    assertTrue(tempFile.delete());
    final CnvProductParams.CnvProductParamsBuilder builder = CnvProductParams.builder();
    builder.mappedBase(new ArrayList<File>());
    builder.mappedTarget(new ArrayList<File>());
    builder.outputParams(new OutputParams(tempFile, false, false));
    final CnvProductParams params = builder.create();
    TestUtils.containsAll(params.toString(), "CnvProductParams",
      "bucketSize=100",
      "baseLineInput=[]",
      "targetInput=[]",
      "filterStartPositions=" + true,
      "divisionFactor=" + 3.0,
      "multiplicationFactor=" + 3.0,
      "OutputParams",
      "directory=" + tempFile.getAbsolutePath(),
      "maxMatedAlignmentScore=null",
      "maxUnmatedAlignmentScore=null",
      "maxAlignmentCount=-1",
      "progress=" + false,
      "zip=" + false);

    assertEquals(tempFile, params.directory());
    assertEquals(new File(tempFile, "file").getPath(), params.file("file").getPath());
  }

  public void testOmnes() {
    new TestParams(CnvProductParams.class, CnvProductParamsBuilder.class).check();
  }

}
