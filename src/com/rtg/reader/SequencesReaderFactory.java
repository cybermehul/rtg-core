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

import java.io.File;
import java.io.IOException;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;
import com.rtg.util.intervals.LongRange;

/**
 * Constructs <code>SequencesReader</code>s.
 *
 */
public final class SequencesReaderFactory {

  private SequencesReaderFactory() {
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param region the sequences of the SDF to include
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReader(final File dir, LongRange region) throws IOException {
    return new DefaultSequencesReader(dir, region);
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   *
   * @param dir the SDF directory
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReader(final File dir) throws IOException {
    return new DefaultSequencesReader(dir, LongRange.NONE);
  }

  /**
   * Constructs a <code>MemorySequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static SequencesReader createMemorySequencesReader(final File dir, final boolean loadNames, LongRange region) throws IOException {
    return createMemorySequencesReader(dir, loadNames, false, region);
  }


  /**
   * Constructs a <code>MemorySequencesReader</code>.
   *
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param loadFullNames whether to load full names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   */
  public static SequencesReader createMemorySequencesReader(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region) throws IOException {
    //new Throwable("With dir " + dir).printStackTrace();
    if (dir == null) {
      return null;
    }
    return CompressedMemorySequencesReader.createSequencesReader(dir, loadNames, loadFullNames, region);
  }

  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReaderCheckEmpty(final File dir) throws IOException, NoTalkbackSlimException {
    return createDefaultSequencesReaderCheckEmpty(dir, LongRange.NONE);
  }


  /**
   * Constructs a <code>DefaultSequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @param region the sequences of the SDF to include
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static synchronized AnnotatedSequencesReader createDefaultSequencesReaderCheckEmpty(final File dir, LongRange region) throws IOException, NoTalkbackSlimException {
    final AnnotatedSequencesReader result = createDefaultSequencesReader(dir, region);
    ReaderUtils.validateNotEmpty(result);
    return result;
  }

  /**
   * Constructs a <code>MemorySequencesReader</code>.
   * Checks if the resulting reader has no sequences.
   * @param dir the SDF directory
   * @param loadNames whether to load names from disk or not
   * @param loadFullNames whether to load full names from disk or not
   * @param region range of sequences to load
   * @return a <code>SequencesReader</code>
   * @throws IOException if another I/O related error occurs
   * @throws NoTalkbackSlimException if the reader has not sequences.
   */
  public static SequencesReader createMemorySequencesReaderCheckEmpty(final File dir, final boolean loadNames, boolean loadFullNames, LongRange region) throws IOException, NoTalkbackSlimException {
    final SequencesReader result = createMemorySequencesReader(dir, loadNames, loadFullNames, region);
    ReaderUtils.validateNotEmpty(result);
    return result;
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param dir SDF directory
   * @param range the range
   * @return resolved range
   * @throws IOException if an IO error occurs
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static LongRange resolveRange(final File dir, LongRange range) throws IOException {
    final IndexFile index = new IndexFile(dir);
    return resolveRange(index, range);
  }

  /**
   * Resolves an inital range (supplied by the user, and may have unbounded ends) to the available sequences.
   * If end is greater than number of sequences it sets end to number of sequences.
   * @param index the SDF index for the reader
   * @param range the range
   * @return the resolved range.
   * @throws NoTalkbackSlimException if the start is out of range.
   */
  public static LongRange resolveRange(IndexFile index, LongRange range) {
    final long start = range.getStart() == LongRange.MISSING ? 0 : range.getStart();
    if (start < 0) {
      throw new IllegalArgumentException();
    }
    if (start > index.getNumberSequences() || (index.getNumberSequences() != 0 && start == index.getNumberSequences())) {  // Allow start == 0 if empty SDF
      throw new NoTalkbackSlimException("The start sequence id \"" + start + "\" must be less than than the number of available sequences \"" + index.getNumberSequences() + "\".");
    }
    long end = range.getEnd() == LongRange.MISSING ? index.getNumberSequences() : range.getEnd();
    if (end > index.getNumberSequences()) {
      Diagnostic.warning("The end sequence id \"" + range.getEnd() + "\" is out of range, it"
        + " must be from \"" + (start + 1) + "\" to \"" + index.getNumberSequences() + "\". Defaulting end to \"" + index.getNumberSequences() + "\"");
      end = index.getNumberSequences();
    }
    return new LongRange(start, end);
  }
}
