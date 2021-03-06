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
package com.rtg.simulation.reads;

import com.rtg.util.io.MemoryPrintStream;

import junit.framework.TestCase;

/**
 * Test class
 */
public class IlluminaPairedEndMachineTest extends TestCase {

  public void testProcessFragment() throws Exception {
    IlluminaPairedEndMachine m = new IlluminaPairedEndMachine(42);
    assertTrue(m.isPaired());
    MemoryPrintStream out = new MemoryPrintStream();
    FastaReadWriter w = new FastaReadWriter(out.printStream());
    m.setReadWriter(w);
    m.setLeftReadLength(5);
    m.setRightReadLength(5);
    byte[] frag = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    m.processFragment("name/", 30, frag, frag.length);
    assertEquals(">0 name/31/F/5./Left\nAAAAA\n>0 name/52/R/5./Right\nTTTTT\n", out.toString());
  }
}
