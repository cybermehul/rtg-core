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
package com.rtg.util.array.intindex;

import java.io.IOException;
import java.io.ObjectOutputStream;

import com.rtg.util.array.AbstractIndex;
import com.rtg.util.format.FormatInteger;

/**
 * Common code used in implementing all the <code>int</code> index variants. Holds some handy
 * constants as well as the length of the index.
 *
 */
public abstract class IntIndex extends AbstractIndex {

  /**
   * Maximum number of bits that can be used when allocating an <code>int</code> array.
   */
  protected static final int MAX_BITS = 29;

  /**
   * Length of largest allocatable <code>int</code> array.
   */
  static final long MAX_LENGTH = 1L << MAX_BITS;

  /**
   * Information used in creating "chunks" in some of the implementations. Be
   * wary of changing CHUNK_BITS.
   */
  protected static final int CHUNK_BITS = 28;

  /**
   * Number of bytes in an <code>int</code>.
   */
  protected static final int INT_SIZE = 4;

  /** Number of a bits in an int. */
  private static final int INT_BITS = 32;

  /** The low order bits of a long corresponding to an int. */
  static final long INT_MASK = (1L << INT_BITS) - 1L;

  /** The bits above those used by an int. */
  static final long HIGH_MASK = ~INT_MASK;

  /** The bits from the signed bit for a int up. */
  static final long HIGH_SIGNED_MASK = ~((1L << (INT_BITS - 1)) - 1L);

  /**
   * @param length of the array.
   * @exception NegativeArraySizeException if length less than 0
   */
  protected IntIndex(final long length) {
    super(length);
  }

  /**
   * @return the number of bytes consumed.
   */
  @Override
  public long bytes() {
    return INT_SIZE * mLength;
  }

  static final FormatInteger FORMAT_VALUE = new FormatInteger(10);

  @Override
  protected FormatInteger formatValue() {
    return FORMAT_VALUE;
  }

  @Override
  public void set(final long index, final long value) {
    //High order bits must be zero
    assert (value & HIGH_MASK) == 0L : value;
    setInt(index, (int) value);
  }

  @Override
  public long get(final long index) {
    return getInt(index) & INT_MASK;
  }

  /**
   * Get the <code>int</code> at the specified index
   *
   * @param index the index
   * @return long value
   * @throws UnsupportedOperationException if the underlying type
   * is not an <code>int</code>.
   */
  public abstract int getInt(final long index);

  /**
   * Set the <code>int</code> at the specified index
   *
   * @param index the index
   * @param value the value
   * @throws UnsupportedOperationException if they underlying type
   * is not an <code>int</code>.
   */
  public abstract void setInt(final long index, final int value);

  /**
   * Save this index such that it can be loaded again from {@link IntCreate#loadIndex(java.io.ObjectInputStream)}
   * @param dos steam to save to
   * @throws IOException if an IO error occurs
   */
  public abstract void save(ObjectOutputStream dos) throws IOException;
}

