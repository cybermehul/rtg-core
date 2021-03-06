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
package com.rtg.reader;

import static com.rtg.util.StringUtils.LS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import com.rtg.launcher.BuildTestUtils;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.TestCase;

/**
 *
 *
 */
public class PrereadNamesTest extends TestCase {
  private File mDir = null;

  /**
   *
   */
  public PrereadNamesTest(final String name) {
    super(name);
  }

  /** Subject sequence used for the calibration runs.  */
  public static final String SEQ_DNA_A1 = ""
    + ">x" + LS
    + "actg" + LS;

  /** Query sequence used for the calibration runs.  */
  public static final String SEQ_DNA_A2 = ""
    + ">u" + LS
    + "actgact" + LS
    + ">v" + LS
    + "antg" + LS;

  public void testViaFile() throws Exception {
    final File queryDir = BuildTestUtils.prereadDNA(mDir, SEQ_DNA_A2);
    final PrereadNames names = new PrereadNames(queryDir, LongRange.NONE);
    assertEquals("u", names.name(0));
    assertEquals("v", names.name(1));
  }

  public void testViaFile2() throws Exception {
    final File queryDir = BuildTestUtils.prereadDNA(mDir, SEQ_DNA_A1);
    final PrereadNames names = new PrereadNames(queryDir, LongRange.NONE);
    assertEquals("x", names.name(0));
  }

  private static final String SEQ = ""
    + ">null+1" + LS
    + "JMLKRTCQRVE" + LS
    + ">null+2" + LS
    + "*CPSERVNEWR" + LS
    + ">null+3" + LS
    + "DAEANVSTSGE" + LS
    ;

  static void writeProtein(final String inputSequence, final File dir) throws IOException {
    final ArrayList<InputStream> inputStreams = new ArrayList<>();
    inputStreams.add(new ByteArrayInputStream(inputSequence.getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(inputStreams,
        new ProteinFastaSymbolTable());
    final SequencesWriter sequenceWriter = new SequencesWriter(ds, dir, 20, PrereadType.UNKNOWN, false);
    sequenceWriter.processSequences();
  }

  public void testRollover() throws Exception {
    final File queryDir = FileUtils.createTempDir("test", "protein");
    try {
      writeProtein(SEQ, queryDir);
      final PrereadNames names = new PrereadNames(queryDir, LongRange.NONE);
      assertEquals("null+1", names.name(0));
      assertEquals("null+2", names.name(1));
      assertEquals("null+3", names.name(2));
      final StringWriter sw = new StringWriter();
      names.writeName(sw, 2);
      names.writeName(sw, 1);
      names.writeName(sw, 0);
      assertEquals("null+3null+2null+1", sw.toString());
      final ByteArrayOutputStream bos = new ByteArrayOutputStream();
      try {
        names.writeName(bos, 1);
        names.writeName(bos, 2);
        names.writeName(bos, 0);
      } finally {
        bos.close();
      }
      assertEquals("null+2null+3null+1", bos.toString());
    } finally {
      FileHelper.deleteAll(queryDir);
    }
  }

  static final String NAMES_FASTA =
            ">0234567890123456" + LS + "ACGT" + LS         //16 0 file0
          + ">1234567890123456" + LS + "ACGT" + LS         //16 0 file1
          + ">223" + LS + "ACGT" + LS                      //3 16 file1
          + ">3234567890123456789" + LS + "ACGT" + LS     //19 0 file2
          + ">423456" + LS + "ACGT" + LS                   //6 0 file3
          + ">523456" + LS + "ACGT" + LS                   //6 6 file3
          + ">623456" + LS + "ACGT" + LS                   //6 12 file3
          + ">723456789012" + LS + "ACGT" + LS             //12 0 file4
          + ">823456" + LS + "ACGT" + LS;                  //6  12 file5

  public void testLoadPointersSplit() throws Exception {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    //long[] seqIndex = {2, 1, 1, 1, 1};
    final DataFileIndex seqIndex = DataFileIndex.loadLabelDataFileIndex(new IndexFile(mDir).dataIndexVersion(), mDir);
    final ArrayList<int[]> pointers = new ArrayList<>();
    PrereadNames.loadPointers(pointers, mDir, 3, 8, seqIndex, false);

    assertEquals(2, pointers.get(0).length);
    assertEquals(0, pointers.get(0)[0]);
    assertEquals(20, pointers.get(0)[1]);

    assertEquals(3, pointers.get(1).length);
    assertEquals(0, pointers.get(1)[0]);
    assertEquals(7, pointers.get(1)[1]);
    assertEquals(14, pointers.get(1)[2]);

    assertEquals(3, pointers.get(2).length);
    assertEquals(0, pointers.get(2)[0]);
    assertEquals(7, pointers.get(2)[1]);
    assertEquals(20, pointers.get(2)[2]);
  }

  public void testLoadPointersSplit2() throws Exception {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    //long[] seqIndex = {2, 1, 1, 1, 1};
    final DataFileIndex seqIndex = DataFileIndex.loadLabelDataFileIndex(new IndexFile(mDir).dataIndexVersion(), mDir);
    //final long[] seqIndex = ArrayUtils.readLongArray(Bsd.labelIndexFile(mDir));
    final ArrayList<int[]> pointers = new ArrayList<>();
    PrereadNames.loadPointers(pointers, mDir, 5, 8, seqIndex, false);
    assertEquals(2, pointers.size());

    assertEquals(2, pointers.get(0).length);
    assertEquals(0, pointers.get(0)[0]);
    assertEquals(7, pointers.get(0)[1]);

    assertEquals(3, pointers.get(1).length);
    assertEquals(0, pointers.get(1)[0]);
    assertEquals(7, pointers.get(1)[1]);
    assertEquals(20, pointers.get(1)[2]);

  }


  public void testPartialLoad() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    PrereadNames names = new PrereadNames(mDir, new LongRange(3, 8));
    assertEquals(5, names.length());
    assertEquals("3234567890123456789", names.name(0));
    assertEquals("423456", names.name(1));
    assertEquals("523456", names.name(2));
    assertEquals("623456", names.name(3));
    assertEquals("723456789012", names.name(4));

  }

  public void testPartialLoad2() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    PrereadNames names = new PrereadNames(mDir, new LongRange(5, 8));
    assertEquals(3, names.length());
    assertEquals("523456", names.name(0));
    assertEquals("623456", names.name(1));
    assertEquals("723456789012", names.name(2));
    assertEquals((7 + 7 + 13) + 5 * 4, names.bytes());
  }

  public void testFullLoad() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    PrereadNames names = new PrereadNames(mDir, LongRange.NONE);

    assertEquals("0234567890123456", names.name(0));
    assertEquals("1234567890123456", names.name(1));
    assertEquals("223", names.name(2));
    assertEquals("3234567890123456789", names.name(3));
    assertEquals("423456", names.name(4));
    assertEquals("523456", names.name(5));
    assertEquals("623456", names.name(6));
    assertEquals("723456789012", names.name(7));
    assertEquals("823456", names.name(8));
    assertTrue(new IndexFile(mDir).getNameChecksum() == names.calcChecksum());

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final StringWriter sb = new StringWriter();
    int[] reads = {4, 5, 3, 8};
    try {
      for (int read : reads) {
        bos.write(" ".getBytes());
        names.writeName(bos, read);
        sb.append(" ");
        names.writeName(sb, read);
      }
    } finally {
      bos.close();
    }
    String expected = " 423456 523456 3234567890123456789 823456";
    assertEquals(expected, bos.toString());
    assertEquals(expected, sb.toString());
  }
  public void testEmpty() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA("", mDir, new SdfId(1L), 20);
    reader.close();
    PrereadNames names = new PrereadNames(mDir, LongRange.NONE);
    assertEquals(0, names.length());
  }

  public void testEmptyRegion() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA(NAMES_FASTA, mDir, new SdfId(1L), 20);
    reader.close();
    PrereadNames names = new PrereadNames(mDir, new LongRange(2, 2));
    assertEquals(0, names.length());
  }

  @Override
  public void setUp() throws Exception {
    mDir = FileHelper.createTempDirectory();
    Diagnostic.setLogStream();
  }

  @Override
  public void tearDown() {
    FileHelper.deleteAll(mDir);
    mDir = null;
  }

}
