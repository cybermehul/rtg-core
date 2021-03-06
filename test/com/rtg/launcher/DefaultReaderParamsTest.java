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
package com.rtg.launcher;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import com.rtg.mode.DNAFastaSymbolTable;
import com.rtg.mode.ProteinFastaSymbolTable;
import com.rtg.mode.SequenceMode;
import com.rtg.reader.FastaSequenceDataSource;
import com.rtg.reader.PrereadType;
import com.rtg.reader.SequencesWriter;
import com.rtg.util.intervals.LongRange;
import com.rtg.util.TestUtils;
import com.rtg.util.Utils;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.SlimException;
import com.rtg.util.io.FileUtils;
import com.rtg.util.io.MemoryPrintStream;
import com.rtg.util.test.FileHelper;
import com.rtg.util.test.MockEventListener;

import junit.framework.TestCase;

/**
 */
public class DefaultReaderParamsTest extends TestCase {

  public static DefaultReaderParams createDefaultReaderParams(final File sequenceDir, LongRange readerRestriction, final SequenceMode mode) {
    return new DefaultReaderParams(sequenceDir, readerRestriction, mode, null, false, false, false);
  }

  public DefaultReaderParamsTest(final String name) {
    super(name);
  }

  private void getReaderDNA(final String inputDnaSequence, final File dir) throws IOException {
    final ArrayList<InputStream> inputStreams = new ArrayList<>();
    inputStreams.add(new ByteArrayInputStream(inputDnaSequence.getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(inputStreams, new DNAFastaSymbolTable());
    final SequencesWriter sequenceWriter = new SequencesWriter(ds, dir, 20, PrereadType.UNKNOWN, false);
    sequenceWriter.processSequences();

  }

  private void getReaderProtein(final String inputProteinSequence, final File dir) throws IOException {
    final ArrayList<InputStream> al = new ArrayList<>();
    al.add(new ByteArrayInputStream(inputProteinSequence.getBytes()));
    final FastaSequenceDataSource ds = new FastaSequenceDataSource(al, new ProteinFastaSymbolTable());
    final SequencesWriter sw = new SequencesWriter(ds, dir, 20, PrereadType.UNKNOWN, false);
    sw.processSequences();

  }

  public void testConstructor() throws IOException {
    Diagnostic.setLogStream();
    final File f = FileUtils.createTempDir("test", "defreadparampro");
    try {
      final File f2 = FileUtils.createTempDir("twst", "defreadparamdna");
      try {
        getReaderProtein(">test\nACNGT\n", f);
        getReaderDNA(">test\nACGT\n", f2);
        final DefaultReaderParams drp = createDefaultReaderParams(f, LongRange.NONE, SequenceMode.PROTEIN);
        final DefaultReaderParams drp2 = createDefaultReaderParams(f, LongRange.NONE, SequenceMode.PROTEIN);
        final DefaultReaderParams drp3 = createDefaultReaderParams(f2, LongRange.NONE, SequenceMode.BIDIRECTIONAL);
        assertTrue(drp.closed());
        assertEquals(SequenceMode.PROTEIN, drp.mode());
        assertEquals(f, drp.directory());
        assertEquals(5, drp.maxLength());
        assertEquals(Utils.hash(new Object[]{SequenceMode.PROTEIN, f, Boolean.FALSE}), drp.hashCode());
        assertEquals("SequenceParams mode=" + SequenceMode.PROTEIN + " directory=" + f + " usememory=" + Boolean.FALSE, drp.toString());
        assertTrue(Arrays.toString(drp.lengths()), Arrays.equals(new int[] {5}, drp.lengths()));
        assertTrue(drp.lengths() == drp.lengths()); //make sure cacheing correctly

        assertTrue(drp.equals(drp));
        assertFalse(drp.equals(null));
        assertTrue(drp.equals(drp2));
        assertFalse(drp.equals(drp3));
        assertTrue(drp.integrity());
        drp.close();
        assertTrue(drp.closed());
        drp2.close();
        assertTrue(drp2.closed());
        drp3.close();
        assertTrue(drp3.closed());
      } finally {
        FileHelper.deleteAll(f2);
      }
    } finally {
      FileHelper.deleteAll(f);
    }
  }

  public void testSloppyEnd() throws IOException {
    final MemoryPrintStream out = new MemoryPrintStream();
    Diagnostic.setLogStream(out.printStream());
    final File f = FileUtils.createTempDir("test", "defreadparampro");
    try {
      getReaderDNA(">test\nACGT\n", f);
      out.reset();
      final DefaultReaderParams drp = createDefaultReaderParams(f, new LongRange(0, 2), SequenceMode.BIDIRECTIONAL);
      final LongRange adjustedRegion = drp.adjustedRegion();
      assertNotNull(adjustedRegion);
      assertEquals(0, adjustedRegion.getStart());
      assertEquals(1, adjustedRegion.getEnd());
      TestUtils.containsAll(out.toString(), "The end sequence id \"2\" is out of range, it must be from \"1\" to \"1\". Defaulting end to \"1\"");
      out.reset();
      final DefaultReaderParams drp1 = createDefaultReaderParams(f, new LongRange(0, 1), SequenceMode.BIDIRECTIONAL);
      assertEquals(1, drp1.adjustedRegion().getEnd());
      assertFalse(out.toString(), out.toString().contains("The end sequence id"));
    } finally {
      Diagnostic.setLogStream();
      FileHelper.deleteAll(f);
    }
  }


  public void testInvalid() throws IOException {
    final MockEventListener ev = new MockEventListener();
    Diagnostic.addListener(ev);
    Diagnostic.setLogStream();
    final File f = FileUtils.createTempDir("test", "defreadparam");
    try {
      getReaderDNA(">test\nACNGT\n", f);
      try {
        final DefaultReaderParams rp = createDefaultReaderParams(f, LongRange.NONE, SequenceMode.PROTEIN);
        rp.reader(); //force creation of reader
        try {
          rp.integrity();
          fail();
        } finally {
          rp.close();
        }
      } catch (final SlimException ex) {
        ex.printErrorNoLog();
        ev.compareErrorMessage("The specified sequence is of type \"DNA\" required \"PROTEIN\".");
      }
    } finally {
      FileHelper.deleteAll(f);
      Diagnostic.removeListener(ev);
    }
  }

  public void testDirectoryNotExist() throws IOException {
    final File tempDir = FileUtils.createTempDir("DefaultReaderParamsTest", null);
    try {
      final MockEventListener ev3 = new MockEventListener();
      Diagnostic.addListener(ev3);
      Diagnostic.setLogStream();

      try {
        final File t = new File(tempDir, "t");
        try {
          final DefaultReaderParams rp = createDefaultReaderParams(t, LongRange.NONE, SequenceMode.BIDIRECTIONAL);
          rp.reader(); //force creation of reader
          rp.integrity();
          fail();
        } catch (final SlimException ex) {
          ex.printErrorNoLog();
          assertTrue(ev3.compareErrorMessage("Error: The specified SDF, \"" + t.toString() + "\", does not exist."));
        }
      } finally {
        Diagnostic.removeListener(ev3);
      }
    } finally {
      FileHelper.deleteAll(tempDir);
    }
  }

  public void testDirectoryInvalid() throws IOException {
    final MockEventListener ev3 = new MockEventListener();
    Diagnostic.addListener(ev3);
    Diagnostic.setLogStream();
    final File dir = FileUtils.createTempDir("defaultreader", "test");
    try {
      try {
        final DefaultReaderParams rp = createDefaultReaderParams(dir, LongRange.NONE, SequenceMode.BIDIRECTIONAL);
        rp.reader(); //force creation of reader
        rp.integrity();
        fail();
      } catch (final SlimException ex) {
        ex.printErrorNoLog();
        assertTrue(ev3.compareErrorMessage("Error: The specified SDF, \"" + dir.toString() + "\", does not seem to contain a valid SDF index."));
      }
    } finally {
      Diagnostic.removeListener(ev3);
    }
    FileHelper.deleteAll(dir);
  }

  public void testDirectoryIsFile() throws IOException {
    final MockEventListener ev3 = new MockEventListener();
    Diagnostic.addListener(ev3);
    Diagnostic.setLogStream();
    final File file = File.createTempFile("defaultreader", "test");
    try {
      try {
        final DefaultReaderParams rp = createDefaultReaderParams(file, LongRange.NONE, SequenceMode.BIDIRECTIONAL);
        rp.reader(); //force creation of reader
        rp.integrity();
        fail();
      } catch (final SlimException ex) {
        ex.printErrorNoLog();
        assertTrue(ev3.compareErrorMessage("Error: The specified file, \"" + file.toString() + "\", is not an SDF."));
      }
    } finally {
      Diagnostic.removeListener(ev3);
    }
    FileHelper.deleteAll(file);
  }
}

