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

package com.rtg.util.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.reeltwo.jumble.annotations.TestClass;

/**
 * A class to help build iterators where it is necessary to do a look ahead to check if the next
 * case is available.
 * @param <X> type the iterator operates over.
 */
@TestClass({"com.rtg.util.iterators.ArrayToIteratorTest", "com.rtg.util.iterators.IteratorHelperTest"})
public abstract class IteratorHelper<X> implements Iterator<X> {

  protected abstract void step();

  protected abstract boolean atEnd();

  protected boolean isOK() {
    return true;
  }

  protected abstract X current();

  @Override
  public final boolean hasNext() {
    check();
    return !atEnd() && isOK();
  }

  private void check() {
    while (!atEnd() && !isOK()) {
      step();
    }
  }

  private void stepAll() {
    step();
    check();
  }

  @Override
  public final X next() {
    check();
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final X res = current();
    stepAll();
    return res;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

}
