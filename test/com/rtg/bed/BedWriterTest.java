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

package com.rtg.bed;

import static com.rtg.util.StringUtils.LS;
import static com.rtg.util.StringUtils.TAB;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.rtg.util.io.FileUtils;
import com.rtg.util.io.TestDirectory;

import junit.framework.TestCase;

/**
 */
public class BedWriterTest extends TestCase {

  private static final String BED_CONTENTS = ""
      + "# COMMENT" + LS
      + "track adsfasdf" + LS
      + "browser adsfasdf" + LS
      + "chr1" + TAB + "2" + TAB + "80" + TAB + "annotation" + LS
      + "chr2" + TAB + "3" + TAB + "40" + TAB + "annotation1" + TAB + "annotation2" + LS
      + "chr3" + TAB + "7" + TAB + "90" + TAB + "annotation" + TAB + "annotation3" + LS;

  public void testBedWriter() throws IOException {
    try (TestDirectory testDir = new TestDirectory()) {
      final File bedFile = new File(testDir, "test.bed");
      try (BedWriter writer = new BedWriter(new FileOutputStream(bedFile))) {
        writer.writeComment(" COMMENT");
        writer.writeLine("track adsfasdf");
        writer.writeLine("browser adsfasdf");
        writer.write(new BedRecord("chr1", 2, 80, "annotation"));
        writer.write(new BedRecord("chr2", 3, 40, "annotation1", "annotation2"));
        writer.write(new BedRecord("chr3", 7, 90, "annotation", "annotation3"));
      }
      final String written = FileUtils.fileToString(bedFile);
      assertEquals(BED_CONTENTS, written);
    }
  }
}
