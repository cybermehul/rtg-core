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
package com.rtg.vcf.header;

import java.util.HashSet;

import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.util.diagnostic.NoTalkbackSlimException;

import junit.framework.TestCase;

/**
 * Test class
 */
public class VcfHeaderMergeTest extends TestCase {

  private static final String VERSION_LINE = VcfHeader.VERSION_LINE;

  private static final String GENERIC_META_LINE_1 = "##something";
  private static final String GENERIC_META_LINE_2 = "##key=val";
  private static final String INFO_META_LINE_1 = "##INFO=<ID=info_1,Number=5,Type=Integer,Description=\"info description\">";
  private static final String INFO_META_LINE_1B = "##INFO=<ID=info_1,Number=5,Type=Float,Description=\"info description\">";
  private static final String INFO_META_LINE_1X = "##INFO=<ID=info_1,Number=6,Type=Integer,Description=\"info description\">";
  private static final String INFO_META_LINE_2 = "##INFO=<ID=info_2,Number=.,Type=String,Description=\"other info description\">";
  private static final String FORMAT_META_LINE_1 = "##FORMAT=<ID=format_1,Number=5,Type=Integer,Description=\"format description\">";
  private static final String FORMAT_META_LINE_1B = "##FORMAT=<ID=format_1,Number=5,Type=Float,Description=\"format description\">";
  private static final String FORMAT_META_LINE_1X = "##FORMAT=<ID=format_1,Number=5,Type=Integer,Description=\"format description is different\">";
  private static final String FORMAT_META_LINE_2 = "##FORMAT=<ID=format_2,Number=.,Type=String,Description=\"other format description\">";
  private static final String FILTER_META_LINE_1 = "##FILTER=<ID=filter_1,Description=\"filter description\">";
  private static final String FILTER_META_LINE_1X = "##FILTER=<ID=filter_1,Description=\"filter description is different\">";
  private static final String FILTER_META_LINE_2 = "##FILTER=<ID=filter_2,Description=\"other filter description\">";

  @Override
  protected void setUp() {
    Diagnostic.setLogStream();
  }

  public void testMerge() {
    final VcfHeader a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(GENERIC_META_LINE_1);
    a.addMetaInformationLine(INFO_META_LINE_1);
    a.addMetaInformationLine(FILTER_META_LINE_1);
    a.addMetaInformationLine(FORMAT_META_LINE_1B);
    a.addSampleName("sample");
    a.addSampleName("x");
    a.addSampleName("a");
    a.addSampleName("c");

    final VcfHeader b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(GENERIC_META_LINE_1);
    b.addMetaInformationLine(GENERIC_META_LINE_2);
    b.addMetaInformationLine(INFO_META_LINE_1B);
    b.addMetaInformationLine(FILTER_META_LINE_2);
    b.addMetaInformationLine(FORMAT_META_LINE_1);
    b.addSampleName("sample");
    b.addSampleName("x");
    b.addSampleName("b");
    b.addSampleName("c");

    //order is meta, filter, info, format
    final String exp = VERSION_LINE + "\n"
            + GENERIC_META_LINE_1 + "\n"
            + GENERIC_META_LINE_2 + "\n"
            + FILTER_META_LINE_1 + "\n"
            + FILTER_META_LINE_2 + "\n"
            + INFO_META_LINE_1B + "\n"
            + FORMAT_META_LINE_1B + "\n"
            + VcfHeader.HEADER_LINE + "\tsample\tx\ta\tc\tb\n";
    final VcfHeader c = VcfHeaderMerge.mergeHeaders(a, b, null);
    assertEquals(exp, c.toString());
  }

  public void testMerge2() {
    final VcfHeader a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(INFO_META_LINE_1);
    a.addMetaInformationLine(FILTER_META_LINE_1);
    a.addMetaInformationLine(FILTER_META_LINE_2);
    a.addMetaInformationLine(FORMAT_META_LINE_2);
    a.addSampleName("sample");
    final VcfHeader b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(INFO_META_LINE_2);
    b.addMetaInformationLine(FILTER_META_LINE_2);
    b.addMetaInformationLine(FORMAT_META_LINE_1);
    b.addMetaInformationLine(FORMAT_META_LINE_2);
    b.addSampleName("sample");
    final String exp = VERSION_LINE + "\n"
            + FILTER_META_LINE_1 + "\n"
            + FILTER_META_LINE_2 + "\n"
            + INFO_META_LINE_1 + "\n"
            + INFO_META_LINE_2 + "\n"
            + FORMAT_META_LINE_2 + "\n"
            + FORMAT_META_LINE_1 + "\n"
            + VcfHeader.HEADER_LINE + "\tsample\n";
    final VcfHeader c = VcfHeaderMerge.mergeHeaders(a, b, null);
    assertEquals(exp, c.toString());
  }

  public void testMerge3() {
    final VcfHeader a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(INFO_META_LINE_1);
    a.addMetaInformationLine(FILTER_META_LINE_1);
    a.addMetaInformationLine(FILTER_META_LINE_2);
    a.addMetaInformationLine(FORMAT_META_LINE_2);
    a.addSampleName("sample");
    final VcfHeader b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(INFO_META_LINE_1X);
    b.addMetaInformationLine(INFO_META_LINE_2);
    b.addMetaInformationLine(FILTER_META_LINE_2);
    b.addMetaInformationLine(FORMAT_META_LINE_1);
    b.addMetaInformationLine(FORMAT_META_LINE_2);
    b.addSampleName("sample");
    final String exp = VERSION_LINE + "\n"
            + FILTER_META_LINE_1 + "\n"
            + FILTER_META_LINE_2 + "\n"
            + INFO_META_LINE_1 + "\n"
            + INFO_META_LINE_2 + "\n"
            + FORMAT_META_LINE_2 + "\n"
            + FORMAT_META_LINE_1 + "\n"
            + VcfHeader.HEADER_LINE + "\tsample\n";
    final HashSet<String> forceMerge = new HashSet<>();
    forceMerge.add("info_1");
    final VcfHeader c = VcfHeaderMerge.mergeHeaders(a, b, forceMerge);
    assertEquals(exp, c.toString());
  }

  public void testMergeError() {
    VcfHeader a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(FORMAT_META_LINE_1);
    a.addMetaInformationLine(INFO_META_LINE_1);
    VcfHeader b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(FORMAT_META_LINE_1X);
    b.addMetaInformationLine(INFO_META_LINE_1X);
    try {
      VcfHeaderMerge.mergeHeaders(a, b, null);
      fail();
    } catch (NoTalkbackSlimException e) {
      assertTrue(e.getMessage().contains("Header line"));
      assertTrue(e.getMessage().contains("incompatible"));
      assertTrue(e.getMessage().contains("format_1"));
      assertTrue(e.getMessage().contains("info_1"));
    }
    a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(INFO_META_LINE_1X);
    b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(INFO_META_LINE_1B);
    try {
      VcfHeaderMerge.mergeHeaders(a, b, null);
      fail();
    } catch (NoTalkbackSlimException e) {
      assertTrue(e.getMessage().contains("Header line"));
      assertTrue(e.getMessage().contains("incompatible"));
      assertTrue(e.getMessage().contains("info_1"));
    }
    a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(FILTER_META_LINE_1);
    b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(FILTER_META_LINE_1X);
    try {
      VcfHeaderMerge.mergeHeaders(a, b, null);
      fail();
    } catch (NoTalkbackSlimException e) {
      assertTrue(e.getMessage().contains("Header line"));
      assertTrue(e.getMessage().contains("incompatible"));
      assertTrue(e.getMessage().contains("filter_1"));
    }

  }

  public void testForce() {
    VcfHeader a = new VcfHeader();
    a.addMetaInformationLine(VERSION_LINE);
    a.addMetaInformationLine(FORMAT_META_LINE_1);
    a.addMetaInformationLine(INFO_META_LINE_1);
    a.addSampleName("sample");
    VcfHeader b = new VcfHeader();
    b.addMetaInformationLine(VERSION_LINE);
    b.addMetaInformationLine(FORMAT_META_LINE_1X);
    b.addMetaInformationLine(INFO_META_LINE_1X);
    b.addSampleName("sample");
    final HashSet<String> forceMerge = new HashSet<>();
    forceMerge.add("format_1");
    forceMerge.add("info_1");
    final String exp = VERSION_LINE + "\n"
            + INFO_META_LINE_1 + "\n"
            + FORMAT_META_LINE_1 + "\n"
            + VcfHeader.HEADER_LINE + "\tsample\n";
    final VcfHeader c = VcfHeaderMerge.mergeHeaders(a, b, forceMerge);
    assertEquals(exp, c.toString());
  }

}
