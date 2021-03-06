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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.rtg.Slim;
import com.rtg.mode.DNA;
import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.DnaUtils;
import com.rtg.mode.Residue;
import com.rtg.mode.SequenceType;
import com.rtg.util.PortableRandom;
import com.rtg.util.TestUtils;
import com.rtg.util.bytecompression.MultiByteArray;
import com.rtg.util.bytecompression.MultiByteArrayTest;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests for CompressedMemorySequencesReader
 */
public class CompressedMemorySequencesReaderTest extends AbstractSequencesReaderTest {

  @Override
  protected SequencesReader createSequencesReader(final File dir) throws IOException {
    return CompressedMemorySequencesReader.createSequencesReader(dir, true, false, LongRange.NONE);
  }

  @Override
  protected boolean canReadTwice() {
    return true;
  }

  public CompressedMemorySequencesReaderTest(final String name) {
    super(name);
  }

  public static Test suite() {
    return new TestSuite(CompressedMemorySequencesReaderTest.class);
  }

  /**
   * Main to run from tests from command line.
   * @param args ignored.
   */
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  public void testCRC() {
    assertEquals((byte) 0x8D, crc(new byte[] {0}));
    assertEquals((byte) 0x1B, crc(new byte[] {1}));
    assertEquals((byte) 0xA1, crc(new byte[] {2}));
    assertEquals((byte) 0x37, crc(new byte[] {3}));
    assertEquals((byte) 0x94, crc(new byte[] {4}));

    assertEquals((byte) 0xBB, crc(new byte[] {4, 0, 1, 3, 2, 3, 1, 4, 0, 2, 3, 3, 3}));
  }

  private byte crc(final byte[] data) {
    final CompressedMemorySequencesReader reader = new CompressedMemorySequencesReader(
      new byte[][] {data}, new String[] {"test"}, new long[] {data.length},
      data.length, data.length, SequenceType.DNA);
    assertTrue(reader.integrity());
    return (byte) reader.getChecksum(0);
  }

  public void testPrereadNames() {
    final ArrayPrereadNames pn = new ArrayPrereadNames(new String[] {"blah", "fah", "rah"});
    assertEquals("fah", pn.name(1));
  }


  private final class MyMemorySequencesReader extends CompressedMemorySequencesReader {
    public MyMemorySequencesReader(final byte[][] data, final String[] labels, final long[] counts,
        final int min, final int max, final SequenceType type) {
      super(data, labels, counts, min, max, type);
      final ArrayPrereadNames arr = new ArrayPrereadNames(new String[]{"someFile"});
      assertEquals("someFile", arr.name(0));
      final MultiByteArray mba = new MultiByteArray(10L);
      //System.err.println(mba.get(1));
      assertEquals(0, MultiByteArrayTest.get1(mba, 1));
    }
  }

  public void testArrayPrereadNames() {
   Diagnostic.setLogStream();
   final String seqString = "acgtcacgtcacgtcacgtcacgtcacgtcacgtc";

    new MyMemorySequencesReader(new byte[][] {DnaUtils.encodeArray(seqString.getBytes())},
        new String[] {"seq1"}, new long[] {35}, 35, 35, SequenceType.DNA);


  }

  public void testRoll() throws Exception {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob the buuilder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();
    final SequencesReader dsr = SequencesReaderFactory.createMemorySequencesReader(mDir, true, LongRange.NONE);
    assertTrue(((CompressedMemorySequencesReader) dsr).integrity());
    try {
      assertEquals(mDir, dsr.path());
      assertTrue(dsr.nextSequence());
      assertEquals("1234567890123456789", dsr.currentName());
      assertEquals(32, dsr.currentLength());
      SequencesWriterTest.checkEquals(dsr, new byte[] {1, 2, 3, 4, 3, 4, 3, 4, 3, 4, 2, 4, 4, 1, 3, 3, 3, 2, 4, 2, 1, 2, 4, 3, 3, 4, 2, 1, 4, 3, 2, 1});
      assertTrue(dsr.nextSequence());
      assertEquals("bob", dsr.currentName());
      assertEquals(17, dsr.currentLength());
      SequencesWriterTest.checkEquals(dsr, new byte[] {4, 1, 3, 4, 4, 2, 1, 3, 2, 1, 4, 2, 3, 1, 4, 2, 1});
      assertTrue(dsr.nextSequence());
      assertEquals("hobos", dsr.currentName());
      assertEquals(20, dsr.currentLength());
      SequencesWriterTest.checkEquals(dsr, new byte[] {1, 2, 2, 2, 2, 1, 2, 2, 2, 2, 1, 2, 1, 1, 1, 2, 2, 2, 1, 1});
    } finally {
      dsr.close();
    }
  }

  public void testInfo() throws IOException {
    //set a command line
    new Slim().intMain(new String[] {"aksfj", "-d", "djfk siduf"}, TestUtils.getNullOutputStream(), TestUtils.getNullPrintStream());

    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(createStream(">123456789012345678901\nacgtgtgtgtcttagggctcactggtcatgca\n>bob-the-builder\ntagttcagcatcgatca\n>hobos r us\naccccaccccacaaacccaa"));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new DNAFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, mDir, 20, PrereadType.UNKNOWN, false);
    sw.setComment("wejksfd boier sakrjoieje");
    sw.processSequences();
    final CompressedMemorySequencesReader msr = (CompressedMemorySequencesReader) SequencesReaderFactory.createMemorySequencesReader(mDir, true, LongRange.NONE);
    checkDetails(msr);
    final CompressedMemorySequencesReader msr2 = (CompressedMemorySequencesReader) msr.copy();
    assertTrue(msr2 != msr);
    checkDetails(msr2);
    assertEquals("wejksfd boier sakrjoieje", msr.comment());
    assertEquals("wejksfd boier sakrjoieje", msr2.comment());
    assertEquals("aksfj -d \"djfk siduf\"", msr.commandLine());
    assertEquals("aksfj -d \"djfk siduf\"", msr2.commandLine());
  }

  private void checkDetails(final CompressedMemorySequencesReader msr) throws IOException {
    assertTrue(msr.integrity());
    assertEquals(-1L, msr.currentSequenceId());
    final StringBuilder sb = new StringBuilder();
    msr.infoString(sb);
    TestUtils.containsAll(sb.toString(),
      "Memory Usage\tbytes\tlength",
//      "24\t69\tSeqData",    TODO: uncomment this after long read FastScorer done.
      "3\t3\tSeqChecksums",
      "66\t3\tNames",
      "32\t4\tPositions"
    );
    assertEquals(mDir, msr.path());
    assertEquals(3, msr.numberSequences());
    msr.seek(1L);
    assertEquals("bob-the-builder", msr.currentName());
    assertEquals(1L, msr.currentSequenceId());
    assertEquals(17, msr.currentLength());
    assertEquals(17L + 20L, msr.lengthBetween(1L, 3L));
    final byte[] read = new byte[100];
    assertEquals(17, msr.readCurrent(read));
    assertEquals(DNA.T.ordinal(), read[0]);
    assertEquals(DNA.A.ordinal(), read[1]);
    assertEquals(DNA.G.ordinal(), read[2]);

    assertTrue(msr.getSdfId().available());
    //this doesnt work because on some platforms (eg MacOSX) the canonical directory and directory are not necessarily the same
    //assertEquals(msr.directory().hashCode(), msr.hashCode());
    assertEquals(SequenceType.DNA, msr.type());
    assertEquals(32L + 17L + 20L, msr.totalLength());
    assertEquals(17L, msr.minLength());
    assertEquals(32L, msr.maxLength());
    final long[] counts = msr.residueCounts();
    assertEquals(5, counts.length);
    assertEquals(18L, counts[DNA.A.ordinal()]);
    assertEquals(0L, counts[DNA.N.ordinal()]);

    assertEquals(true, msr.hasHistogram());
    final long[] hist = msr.histogram();
    assertEquals(3L, hist[0]);
    assertEquals(0L, hist[1]);
    assertEquals(0L, msr.nBlockCount());
    assertEquals(0L, msr.longestNBlock());
    assertNotNull(msr.posHistogram());
    assertEquals(0.0, msr.globalQualityAverage());
    assertNotNull(msr.positionQualityAverage());
    assertEquals(PrereadArm.UNKNOWN, msr.getArm());
    assertEquals(PrereadType.UNKNOWN, msr.getPrereadType());

    // seek beyond last sequence
    try {
      msr.seek(3L);
      fail();
    } catch (IllegalArgumentException e) {
      //expected
    }
    // must leave it positioned at a valid sequence
    msr.seek(2L);
    assertEquals(2L, msr.currentSequenceId());
  }

  public void testFencePost() throws IOException {
    final File dir = FileUtils.createTempDir("cmsrt", "fencepost");
    try {
      randomSDF(dir, 256, 8200, 2L * 1024 * 1024);
      final CompressedMemorySequencesReader reader = (CompressedMemorySequencesReader) CompressedMemorySequencesReader.createSequencesReader(dir, true, false, LongRange.NONE);
      assertTrue(reader.checkChecksums());
    } finally {
      assertTrue(FileHelper.deleteAll(dir));
    }
  }

  private static void randomSDF(final File dir, final int seqLength, final int seqNum, final long sizeLimit) throws IOException {
    final PortableRandom rand = new PortableRandom(1);
    final Residue[] bases = {DNA.A, DNA.C, DNA.G, DNA.T};
    final SdfWriter writer = new SdfWriter(dir, sizeLimit, PrereadType.UNKNOWN, false, true, false, SequenceType.DNA);
    final byte[] buf = new byte[seqLength];
    for (int i = 0; i < seqNum; i++) {
      writer.startSequence("random " + i);
      for (int j = 0; j < seqLength; j++) {
        buf[j] = (byte) bases[rand.nextInt(4)].ordinal();
      }
      writer.write(buf, null, buf.length);
      writer.endSequence();
    }
    writer.close();
  }

  static final String POSITIONS_FASTQ = "@r0" + LS + "ACGTACGTACGTACGT" + LS //16 0 file0
          + "+" + LS + "ZZZZZZZZZZZZZZZZ" + LS
          + "@r1" + LS + "ACGTACGTACGTACGT" + LS                               //16 16 file0
          + "+" + LS + "XXXXXXXXXXXXXXXX" + LS
          + "@r2" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 32 file1
          + "+" + LS + "BBBBBBBBBBBBBBBBBBBB" + LS
          + "@r3" + LS + "ACGTACGTACGTACGTACGT" + LS                           //20 52 file2
          + "+" + LS + "YYYYYYYYYYYYYYYYYYYY" + LS
          + "@r4" + LS + "ACGTACGTACGT" + LS                                   //12 72 file3
          + "+" + LS + "DDDDDDDDDDDD" + LS
          + "@r5" + LS + "ACGTAC" + LS                                        //6  84 file4
          + "+" + LS + "EEEEEE" + LS;
  public void testPartialFastq() throws Exception {
    SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(POSITIONS_FASTQ, mDir, false);
    reader.close();

    CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, new LongRange(3, 5));

    cmsr.nextSequence();
    checkFastq(cmsr, "r3", "ACGTACGTACGTACGTACGT");
    cmsr.nextSequence();
    checkFastq(cmsr, "r4", "ACGTACGTACGT");


  }

  public void testOverEndFastq() throws Exception {
    SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(POSITIONS_FASTQ, mDir, false);
    reader.close();

    new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, new LongRange(3, 8));
    // Now this is OK rather than Exception, since our std behaviour is to warn and clip end that are too high back to the available num seqs
  }

  void checkFastq(CompressedMemorySequencesReader cmsr, String name, String read) throws IOException {
      byte[] readBytes = new byte[cmsr.currentLength()];
      cmsr.readCurrent(readBytes);
      assertEquals(name, cmsr.currentName());
      assertEquals(read, DnaUtils.bytesToSequenceIncCG(readBytes));
      cmsr.readCurrentQuality(readBytes);
  }

  static final String SMALL_FASTQ = "@r0" + LS + "ACGTACGTACGTACGT" + LS //16 0 file0
          + "+" + LS + "ABCDEFGHIJKLMNOP" + LS;
  public void testRanges() throws Exception {
    final SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(SMALL_FASTQ, mDir, 20, false);
    reader.close();

    final CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, LongRange.NONE);
    byte[] foo = new byte[6];
    cmsr.readQuality(0, foo, 4, 6);
    String qual = "EFGHIJ";
    for (int i = 0; i < foo.length; i++) {
      assertEquals(qual.charAt(i) - '!', foo[i]);
    }
  }

  public void testFastqEnd() throws Exception {
    SequencesReader reader = ReaderTestUtils.getReaderDNAFastq(POSITIONS_FASTQ, mDir, 20, false);
    reader.close();

    CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, new LongRange(5, 6));

    cmsr.nextSequence();
    checkFastq(cmsr, "r5", "ACGTAC");
    assertFalse(cmsr.nextSequence());
  }

  @Override
  public void testEquals() throws IOException {
    final File dir = FileHelper.createTempDirectory(mDir);
    final File otherDir = FileHelper.createTempDirectory(mDir);
    ReaderTestUtils.getReaderDNAFastq("", dir, false).close();
    ReaderTestUtils.getReaderDNAFastq("", otherDir, false).close();

    CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(dir, new IndexFile(dir), 5, true, false, new LongRange(0, 0));
    CompressedMemorySequencesReader other = new CompressedMemorySequencesReader(otherDir, new IndexFile(dir), 5, true, false, new LongRange(0, 0));
    assertTrue(cmsr.equals(cmsr));
    assertFalse(cmsr.equals(null));
    assertFalse(cmsr.equals(other));
    assertFalse(cmsr.equals("FDSA"));
  }

  public void testEmptyFastq() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNAFastq("", mDir, false);
    reader.close();
    CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, new LongRange(0, 0));
    assertEquals(0, cmsr.numberSequences());

  }
  public void testEmptyFasta() throws IOException {
    SequencesReader reader = ReaderTestUtils.getReaderDNA("", mDir, new SdfId(0L));
    reader.close();
    CompressedMemorySequencesReader cmsr = new CompressedMemorySequencesReader(mDir, new IndexFile(mDir), 5, true, false, new LongRange(0, 0));
    assertEquals(0, cmsr.numberSequences());

  }

  public void testReadMeNoDirectory() throws IOException {
    final CompressedMemorySequencesReader msr = new CompressedMemorySequencesReader(new byte[][] {DnaUtils.encodeArray("acgtcacgtcacgtcacgtcacgtcacgtcacgtc".getBytes())}, new String[] {"seq1"}, new long[] {35}, 35, 35, SequenceType.DNA);
    assertNull(msr.getReadMe());
  }

}
