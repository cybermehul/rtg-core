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

package com.rtg.variant.eval;

import java.io.File;
import java.io.IOException;

import com.rtg.launcher.AbstractCli;
import com.rtg.launcher.AbstractCliTest;
import com.rtg.reader.ReaderTestUtils;
import com.rtg.reader.SdfId;
import com.rtg.tabix.TabixIndexer;
import com.rtg.tabix.UnindexableDataException;
import com.rtg.util.StringUtils;
import com.rtg.util.TestUtils;
import com.rtg.util.cli.CFlags;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.io.FileUtils;
import com.rtg.util.test.FileHelper;
import com.rtg.vcf.header.VcfHeader;

/**
 */
public class VcfEvalCliTest extends AbstractCliTest {
  private File mDir = null;

  @Override
  public void setUp() throws IOException {
    super.setUp();
    mDir = FileUtils.createTempDir("MutationEval", "mDir");
    Diagnostic.setLogStream(TestUtils.getNullPrintStream());

  }

  @Override
  public void tearDown() throws IOException {
    super.tearDown();
    assertTrue(FileHelper.deleteAll(mDir));
    mDir = null;
    Diagnostic.setLogStream();
  }


  @Override
  protected AbstractCli getCli() {
    return new VcfEvalCli();
  }

  public void testInitParams() {
    checkHelp("vcfeval [OPTION]... -b FILE -c FILE -o DIR -t SDF"
        , "Evaluates"
        , "called variants"
        , "baseline variants"
        , "directory for output"
        , "reference genome"
        , "--sample", "the name of the sample to select"
        , "-f,", "--vcf-score-field", "the name of the VCF FORMAT field to use as the ROC score"
        , "-O,", "--sort-order", "the order in which to sort the ROC scores"
        , "--no-gzip", "-Z,", "do not gzip the output"
              );
  }

  public void testValidator() throws IOException {
    final VcfEvalCli.VcfEvalFlagsValidator v = new VcfEvalCli.VcfEvalFlagsValidator();
    final CFlags flags =  new CFlags();
    VcfEvalCli.initFlags(flags);
    final File out = new File(mDir, "out");
    final File calls = new File(mDir, "calls");
    final File mutations = new File(mDir, "mutations");
    final File template = new File(mDir, "template");
    final String[] flagStrings = {
        "-o" , out.getPath()
        , "-c" , calls.getPath()
        , "-b" , mutations.getPath()
        , "-t" , template.getPath()
    };
    flags.setValidator(null);
    flags.setFlags(flagStrings);
    assertFalse(v.isValid(flags));
    assertTrue(flags.getParseMessage().contains("baseline VCF file doesn't exist"));

    assertTrue(mutations.createNewFile());
    assertFalse(v.isValid(flags));
    assertTrue(flags.getParseMessage().contains("calls VCF file doesn't exist"));

    assertTrue(calls.createNewFile());
    assertFalse(v.isValid(flags));

    ReaderTestUtils.getDNADir(">t" + StringUtils.LS + "ACGT" + StringUtils.LS, template);
    assertTrue(v.isValid(flags));


    assertTrue(out.mkdir());
    assertFalse(v.isValid(flags));

    FileHelper.deleteAll(out);

  }

  public void testEmpty() throws IOException, UnindexableDataException {
    final File out = new File(mDir, "out");
    final File calls = new File(mDir, "calls.vcf.gz");
    FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", calls);
    new TabixIndexer(calls).saveVcfIndex();
    final File mutations = new File(mDir, "mutations.vcf.gz");
    FileHelper.stringToGzFile(VcfHeader.MINIMAL_HEADER + "\tSAMPLE\n", mutations);
    new TabixIndexer(mutations).saveVcfIndex();
    final File template = new File(mDir, "template");
    ReaderTestUtils.getReaderDNA(">t" + StringUtils.LS + "A", template, null);
    final String[] flagStrings = {
        "-o" , out.getPath()
        , "-c" , calls.getPath()
        , "-b" , mutations.getPath()
        , "-t" , template.getPath()
    };
    assertEquals(0, new VcfEvalCli().mainInit(flagStrings, TestUtils.getNullOutputStream(), System.err /*SimpleTestUtils.getNullPrintStream()*/));
  }

  public void testNanoSmall() throws IOException, UnindexableDataException {
    check("mutationEvalCli_small");
  }

  private void check(String id) throws IOException, UnindexableDataException {
    final File template = new File(mDir, "template");
    final File mutations = new File(mDir, "mutations.vcf.gz");
    final File calls = new File(mDir, "calls.vcf.gz");
    final File output = new File(mDir, "output");
    ReaderTestUtils.getReaderDNA(mNano.loadReference(id + "_template.fa"), template, new SdfId(0));
    FileHelper.stringToGzFile(mNano.loadReference(id + "_mutations.vcf"), mutations);
    FileHelper.stringToGzFile(mNano.loadReference(id + "_calls.vcf"), calls);
    new TabixIndexer(mutations).saveVcfIndex();
    new TabixIndexer(calls).saveVcfIndex();
    final String out = checkMainInitWarn("-o", output.getPath(), "-c", calls.getPath(), "--sample", "sample1", "-b", mutations.getPath(), "-t", template.getPath(), "-Z");
    mNano.check(id + "_weighted_slope.txt", FileUtils.fileToString(new File(output, "weighted_slope.tsv")));
    mNano.check(id + "out.txt", out);
  }
}
