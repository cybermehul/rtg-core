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
package com.rtg.launcher;

import java.io.IOException;

import com.rtg.util.InvalidParamsException;
import com.rtg.util.Params;
import com.rtg.util.TestUtils;


/**
 * Abstract tests for ParamsCli subclasses
 *
 * @param <P> the type of Params returned by the ParamsCli being tested.
 */
public abstract class AbstractParamsCliTest<P extends Params> extends AbstractCliTest {

  protected ParamsCli<P> mParamsCli;

  protected abstract ParamsCli<P> getParamsCli();


  @Override
  protected AbstractCli getCli() {
    mParamsCli = getParamsCli();
    return mParamsCli;
  }

  @Override
  public void tearDown() throws IOException {
    super.tearDown();
    mParamsCli = null;    // Set by super.setUp();
  }

  protected P checkMakeParamsOut(String... args) throws InvalidParamsException, IOException {
    assertTrue(mCli.handleFlags(args, TestUtils.getNullPrintStream(), TestUtils.getNullPrintStream()));
    return mParamsCli.makeParams();
  }
}
