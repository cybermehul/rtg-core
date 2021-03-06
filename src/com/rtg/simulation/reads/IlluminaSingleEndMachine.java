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

import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.variant.AbstractMachineErrorParams;

/**
 * Illumina read generator machine implementation
 */
public class IlluminaSingleEndMachine extends AbstractIlluminaMachine {

  protected int mReadLength;

  /**
   * Constructs with seed and default Illumina priors
   * @param randomSeed random seed
   * @throws InvalidParamsException if fails to construct priors
   * @throws IOException whenever
   */
  public IlluminaSingleEndMachine(long randomSeed) throws InvalidParamsException, IOException {
    super(randomSeed);
  }

  /**
   * Constructs with seed and specific priors
   * @param params priors to use
   * @param randomSeed random seed
   */
  public IlluminaSingleEndMachine(AbstractMachineErrorParams params, long randomSeed) {
    super(params, randomSeed);
  }

  /**
   * Sets length of generated reads
   * @param val the length
   */
  public void setReadLength(int val) {
    mReadLength = val;
    mQualityBytes = new byte[mReadLength];
    mReadBytes = new byte[mReadLength];
    mWorkspace = new int[Math.max(4, mReadLength)];     //TODO perhaps should be 4+readlength?
  }

  @Override
  public void processFragment(String id, int fragmentStart, byte[] data, int length) throws IOException {
    reseedErrorRandom(mFrameRandom.nextLong());
    final String name = generateRead(id, fragmentStart, data, length, mFrameRandom.nextBoolean(), mReadLength);
    mReadWriter.writeRead(name, mReadBytes, mQualityBytes, mReadLength);
    mResidueCount += mReadLength;
  }

  @Override
  public boolean isPaired() {
    return false;
  }

}
