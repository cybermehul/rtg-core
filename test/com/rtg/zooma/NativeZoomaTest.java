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
package com.rtg.zooma;

import java.io.File;
import java.io.IOException;

import com.rtg.util.test.RandomDna;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class NativeZoomaTest extends TestCase {

  public void test() throws IOException {
    if (NativeZooma.isEnabled()) {
      try (TestDirectory dir = new TestDirectory()) {
        final String reference = RandomDna.random(1000);
        final File referenceFile = new File(dir, "reference.fasta");
        final File indexFile = new File(dir, "reference.bin");
        FileUtils.stringToFile(">reference\n" + reference + "\n", referenceFile);
        final NativeZooma zooma = new NativeZooma();

        zooma.buildIndex(indexFile.getPath(), referenceFile.getPath(), null, null);

      }
    } else {
      System.err.println("cannot run zooma test");
    }

  }
}
