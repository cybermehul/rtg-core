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

package com.rtg.simulation.variants;

import com.rtg.util.PortableRandom;
import com.rtg.util.test.NotRandomRandom;

import junit.framework.TestCase;

/**
 */
public class MutatorTest extends TestCase {

  public void test1() {
    final Mutator mu = new Mutator("XX:DD");
    mu.integrity();
    assertEquals("Mutator:XX:DD", mu.toString());
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult mr = mu.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra);
    assertEquals("2:CG:", mr.toString());
  }

  public void test1Underscore() {
    final Mutator mu = new Mutator("XX_DD");
    mu.integrity();
    assertEquals("Mutator:XX:DD", mu.toString());
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult mr = mu.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra);
    assertEquals("2:CG:", mr.toString());
  }


  public void test2() {
    final Mutator mu = new Mutator("XX");
    mu.integrity();
    assertEquals("Mutator:XX", mu.toString());
    final PortableRandom ra = new NotRandomRandom();
    final MutatorResult mr = mu.generateMutation(new byte[] {1, 2, 3, 4}, 0, ra);
    assertEquals("2:CG:CG", mr.toString());
  }
}