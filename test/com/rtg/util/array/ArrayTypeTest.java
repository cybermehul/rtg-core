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
package com.rtg.util.array;

import static com.rtg.util.array.ArrayType.BYTE;
import static com.rtg.util.array.ArrayType.INTEGER;
import static com.rtg.util.array.ArrayType.LONG;
import static com.rtg.util.array.ArrayType.SHORT;
import static com.rtg.util.array.ArrayType.ZERO;

import com.rtg.util.array.ArrayType.ArrayTypeBit;
import com.rtg.util.array.ArrayType.ArrayTypeBits;
import com.rtg.util.array.bitindex.BitIndex;
import com.rtg.util.array.byteindex.ByteIndex;
import com.rtg.util.array.zeroindex.ZeroIndex;

import junit.framework.TestCase;

/**
 */
public class ArrayTypeTest extends TestCase {

  public final void testBestForBits() {
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(0).createUnsigned(0) instanceof ZeroIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(1).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(2).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(3).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(4).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(5).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(7).createUnsigned(0) instanceof ByteIndex);
    assertTrue(ArrayType.bestForBitsAndSafeFromWordTearing(8).createUnsigned(0) instanceof ByteIndex);
    assertEquals(SHORT, ArrayType.bestForBitsAndSafeFromWordTearing(9));
    assertEquals(SHORT, ArrayType.bestForBitsAndSafeFromWordTearing(15));
    assertEquals(SHORT, ArrayType.bestForBitsAndSafeFromWordTearing(16));
    assertEquals(INTEGER, ArrayType.bestForBitsAndSafeFromWordTearing(17));
    assertEquals(INTEGER, ArrayType.bestForBitsAndSafeFromWordTearing(32));
    assertEquals(LONG, ArrayType.bestForBitsAndSafeFromWordTearing(33));
    assertEquals(LONG, ArrayType.bestForBitsAndSafeFromWordTearing(64));
  }

  public final void testBestForBitsFanatical() {
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(0).createUnsigned(0) instanceof ZeroIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(1).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(2).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(3).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(4).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(5).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(7).createUnsigned(0) instanceof BitIndex);
    assertTrue(ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(8).createUnsigned(0) instanceof ByteIndex);
    assertEquals(SHORT, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(9));
    assertEquals(SHORT, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(15));
    assertEquals(SHORT, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(16));
    assertEquals(INTEGER, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(17));
    assertEquals(INTEGER, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(32));
    assertEquals(LONG, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(33));
    assertEquals(LONG, ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(64));
    try {
      ArrayType.bestForBitsSpaceEfficientButNotSafeFromWordTearing(65);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Cannot store more than 64 bits per entry bits=65", e.getMessage());
    }
  }

  /**
   * Test method for {@link com.rtg.util.array.ArrayType#bestForLength(long)}.
   */
  public final void testBestForLength() {
    assertEquals(ZERO, ArrayType.bestForLength(0L));
    assertEquals(BYTE, ArrayType.bestForLength(1L));
    assertEquals(BYTE, ArrayType.bestForLength(3L));
    assertEquals(BYTE, ArrayType.bestForLength(1L << 7));
    assertEquals(SHORT, ArrayType.bestForLength(1L << 15));
    assertEquals(INTEGER, ArrayType.bestForLength(1L << 16));
    assertEquals(INTEGER, ArrayType.bestForLength((1L << 16) + 1));
    assertEquals(INTEGER, ArrayType.bestForLength(1L << 31));
    assertEquals(LONG, ArrayType.bestForLength(1L << 32));
    assertEquals(LONG, ArrayType.bestForLength((1L << 32) + 1));
    assertEquals(LONG, ArrayType.bestForLength(1L << 49));
    assertEquals(LONG, ArrayType.bestForLength((1L << 62) + 1));
    assertEquals(LONG, ArrayType.bestForLength(Long.MAX_VALUE));
  }

  /**
   * Test method for {@link com.rtg.util.array.ArrayType#bestForBitsAndSafeFromWordTearing(int)}.
   */
  public final void testBestForBitsBad() {
    try {
      ArrayType.bestForBitsAndSafeFromWordTearing(-1);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("bits should be positive:-1", e.getMessage());
    }
    try {
      ArrayType.bestForBitsAndSafeFromWordTearing(65);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("Cannot store more than 64 bits per entry bits=65", e.getMessage());
    }
  }

  public final void testBestForLengthBad() {
    try {
      ArrayType.bestForLength(-1);
      fail();
    } catch (final RuntimeException e) {
      assertEquals("length should be positive:-1", e.getMessage());
    }
  }

  /**
   * Test method for {@link com.rtg.util.array.ArrayType#createUnsigned(long)}.
   */
  public final void testCreate() {
    check(0);
    check(100);
    check(10000);
  }

  private void check(final long len) {
    check(ArrayType.LONG, len);
    check(ArrayType.INTEGER, len);
    check(ArrayType.SHORT, len);
    check(ArrayType.BYTE, len);
    check(ArrayType.ZERO, len);
  }

  private void check(final ArrayType at, final long len) {
    final CommonIndex ix = at.createUnsigned(len);
    assertEquals(ix.bytes(), at.bytes(len));
  }

  public void testZero() {
    final ExtensibleIndex ix = ArrayType.ZERO.createUnsigned(1);
    assertEquals(0, ix.get(0));
  }

  public void testUnimportant() {
    final ArrayType blah = new ArrayType("Name") {
      @Override
      public ExtensibleIndex createUnsigned(long length) {
        return null;
      }
    };
    assertEquals(0, blah.bytes(40));
  }

  public void testArrayTypeBit() {
    final ArrayTypeBit bit = new ArrayTypeBit(3);
    assertTrue(bit.createUnsigned(2) instanceof CommonIndex);
    assertEquals("Bits:3", bit.toString());
    assertEquals(4, bit.bytes(65));
    assertEquals(0, bit.bytes(0));
  }

  public void testArrayTypeBits() {
    ArrayTypeBits bits = new ArrayTypeBits(3);
    assertTrue(bits.createUnsigned(2) instanceof CommonIndex);
    assertEquals("Bits:4", bits.toString());
    assertEquals(40, bits.bytes(64));
    assertEquals(8, bits.bytes(0));
    bits = new ArrayTypeBits(1);
    assertEquals(16, bits.bytes(64));
  }

  public void testBadBytes() {
    try {
      ArrayType.LONG.bytes(-1);
      fail("exception expected");
    } catch (final RuntimeException e) {
      assertEquals("size must be positive:-1", e.getMessage());
    }
  }
}

