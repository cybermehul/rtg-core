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

package com.rtg.reference;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.rtg.reader.SequencesReader;
import com.rtg.reader.SequencesReaderFactory;
import com.rtg.util.Pair;
import com.rtg.util.intervals.RegionRestriction;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

/**
 * Supplies sex dependent information about a reference genome.
 * It uses the names and lengths of sequences in an SDF directory and a tab separated reference file
 * containing the details.
 * <p>
 * Blank lines (containing only white space) and any characters following (and including) a "#" are ignored.
 * The first line starts with a keyword "version", with the second field being a string containing no white space.
 * It is assumed that version numbers are ordered on normal String collating order.
 * Each remaining line starts with a sex field (one of &quot;male&quot;, &quot;female&quot;, &quot;either&quot;)
 * and then a type specifier one of <code>&quot;def&quot;</code> (default), <code>&quot;seq&quot</code>; (sequence),
 * <code>&quot;dup&quot;</code> (duplicate).
 * The order of these lines is immaterial.
 */
public class ReferenceGenome {

  /** Name of reference file within SDF. */
  public static final String REFERENCE_FILE = "reference.txt";

  /** Default reference contents. */
  public static final String REFERENCE_DEFAULT_DIPLOID = "#Default diploid\nversion 0\neither\tdef\tdiploid\tlinear\n";

  /** Default reference contents. */
  public static final String REFERENCE_DEFAULT_HAPLOID = "#Default haploid\nversion 0\neither\tdef\thaploid\tlinear\n";

  /**
   * Default fall-back method enumeration
   */
  public static enum DefaultFallback {
    /** Use diploid default reference */
    DIPLOID,
    /** Use haploid default reference */
    HAPLOID
  }

  /**
   * Get all the sequence names and their lengths.
   * @param genome reader for SDF file.
   * @return the map of names to lengths.
   */
  static LinkedHashMap<String, Integer> names(final SequencesReader genome) {
    final LinkedHashMap<String, Integer> names = new LinkedHashMap<>();
    final long numberSequences = genome.numberSequences();
    try {
      for (long l = 0; l < numberSequences; l++) {
        names.put(genome.name(l), genome.length(l));
      }
    } catch (final IOException e) {
      throw new RuntimeException("Error while reading names from genome. " + genome.path().getAbsolutePath(), e);
    }
    return names;
  }

  private LinkedHashMap<String, ReferenceSequence> mReferences;

  /**
   * This is the constructor that code should typically call when a SequencesReader is already available.
   *
   * @param genome SDF containing sequences and reference specification file.
   * @param sex for this instance.
   * @throws IOException when actual I/O error or problems in file definition.
   */
  public ReferenceGenome(final SequencesReader genome, final Sex sex) throws IOException {
    this(genome, sex, null);
  }

  /**
   * This is the constructor that code should typically call when a SequencesReader is already available.
   *
   * @param genome SDF containing sequences and reference specification file.
   * @param sex for this instance.
   * @param fallback create a default ReferenceGenome of the given type when no reference file is present, if null and there is no reference an error will be thrown
   * @throws IOException when actual I/O error or problems in file definition.
   */
  public ReferenceGenome(final SequencesReader genome, final Sex sex, DefaultFallback fallback) throws IOException {
    final File ref = new File(genome.path(), REFERENCE_FILE);
    final BufferedReader r;
    if (ref.exists()) {
      final InputStreamReader isr = new InputStreamReader(new BufferedInputStream(new FileInputStream(ref)));
      r = new BufferedReader(isr);
    } else if (fallback == DefaultFallback.DIPLOID) {
      r = new BufferedReader(new StringReader(REFERENCE_DEFAULT_DIPLOID));
    } else if (fallback == DefaultFallback.HAPLOID) {
      r = new BufferedReader(new StringReader(REFERENCE_DEFAULT_HAPLOID));
    } else {
      if (genome.path() != null) {
        throw new IOException("No reference file contained in: " + genome.path().getPath());
      } else {
        throw new IOException("No reference file found");
      }
    }
    try {
      parse(names(genome), r, sex);
    } finally {
      r.close();
    }
  }

  /**
   * Constructor for testing.
   * @param genome with names of sequences.
   * @param reference contents of description of reference genome.
   * @param sex for this instance.
   * @throws IOException when actual I/O error or problems in file definition.
   */
  public ReferenceGenome(final SequencesReader genome, final Reader reference, final Sex sex) throws IOException {
    parse(names(genome), reference, sex);
  }

  private void parse(Map<String, Integer> names, final Reader reference, final Sex sex) throws IOException {
    final BufferedReader in = new BufferedReader(reference);
    final ReferenceParse parse = new ReferenceParse(names, in, sex);

    try {
      parse.parse();
    } catch (final IOException e) {
      throw new IOException("I/O error while reading reference file. ", e);
    }

    postProcessing(names, parse);
    if (parse.mError) {
      throw new NoTalkbackSlimException("Invalid reference file (see earlier warning messages).");
    }
  }

  /**
   * Finish constructing the references and link in duplicates.
   * @param names the ordered set of reference sequence names
   * @param parse results from parsing.
   */
  private void postProcessing(Map<String, Integer> names, final ReferenceParse parse) {
    mReferences = new LinkedHashMap<>();
    // deal with sequences in genome but explicitly described.
    for (final Map.Entry<String, Integer> entry : names.entrySet()) {
      final String name = entry.getKey();
      if (parse.mReferences.containsKey(name)) { // have seen explicitly declared seq line, so copy from parse
        mReferences.put(name, parse.mReferences.get(name));
        continue;
      }
      if (parse.mPloidyDefault == null) {
        parse.error("No default specified but required for sequence:" + name);
        return;
      }
      try {
        mReferences.put(name, new ReferenceSequence(false, parse.mLinearDefault, parse.mPloidyDefault, name, null, entry.getValue()));
      } catch (final IllegalArgumentException e) {
        throw new NoTalkbackSlimException("Invalid reference file. " + e.getMessage());
      }
    }

    // add duplicate information into sequence entries
    for (final Pair<RegionRestriction, RegionRestriction> duplicate : parse.mDuplicates) {
      final RegionRestriction r1 = duplicate.getA();
      if (!mReferences.containsKey(r1.getSequenceName())) {
        continue;
      }
      final RegionRestriction r2 = duplicate.getB();
      if (!mReferences.containsKey(r2.getSequenceName())) {
        continue;
      }
      try {
        mReferences.get(r1.getSequenceName()).addDuplicate(duplicate);
        mReferences.get(r2.getSequenceName()).addDuplicate(duplicate);
      } catch (final IllegalArgumentException e) {
        throw new NoTalkbackSlimException("Invalid reference file. " + e.getMessage());
      }
    }
  }

  /**
   * Get all the <code>ReferenceSequence</code>s valid for the specified sex.
   * @return a collection of <code>ReferenceSequences</code> valid for the specified sex.
   */
  public Collection<ReferenceSequence> sequences() {
    return mReferences.values();
  }

  /**
   * Get the <code>ReferenceSequence</code> selected by name.
   * @param name of sequence.
   * @return corresponding <code>ReferenceSequence</code> or null if there is none.
   */
  public ReferenceSequence sequence(final String name) {
    return mReferences.get(name);
  }

  @Override
  public String toString() {
    return toString(mReferences);
  }

  static String toString(final LinkedHashMap<String, ReferenceSequence> references) {
    final StringBuilder sb = new StringBuilder();
    for (final Map.Entry<String, ReferenceSequence> entry : references.entrySet()) {
      sb.append(entry.getValue().toString());
    }
    return sb.toString();
  }

  /**
   * Test if reference file exists
   * @param sr sequences reader
   * @return true if it exists
   */
  public static boolean hasReferenceFile(SequencesReader sr) {
    return new File(sr.path(), REFERENCE_FILE).exists();
  }

  /**
   * Check that a reference.txt file inside an SDF directory is valid for both male and female.
   * @param args command line arguments - SDF directory.
   * @throws IOException during reading of SDF or reference file.
   */
  public static void main(final String[] args) throws IOException {
    final File directory = new File(args[0]);
    final SequencesReader sr = SequencesReaderFactory.createDefaultSequencesReader(directory);
    final ReferenceGenome male = new ReferenceGenome(sr, Sex.MALE);
    System.out.println("male");
    System.out.println(male.toString());
    System.out.println();

    final ReferenceGenome female = new ReferenceGenome(sr, Sex.FEMALE);
    System.out.println("female");
    System.out.println(female.toString());
    System.out.println();

    final ReferenceGenome either = new ReferenceGenome(sr, Sex.EITHER);
    System.out.println("either");
    System.out.println(either.toString());
    System.out.println();


  }
}
