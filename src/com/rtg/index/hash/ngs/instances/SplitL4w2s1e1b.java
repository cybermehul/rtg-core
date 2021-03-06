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
package com.rtg.index.hash.ngs.instances;

import java.io.IOException;

import com.rtg.index.hash.ngs.HashFunctionFactory;
import com.rtg.index.hash.ngs.ImplementHashFunction;
import com.rtg.index.hash.ngs.NgsHashFunction;
import com.rtg.index.hash.ngs.ReadCall;
import com.rtg.index.hash.ngs.TemplateCall;

/**
 * Intended for testing.
 * <br>
 * <table>
 * <tr> <th>window</th> <th> id </th> </tr>
 * <tr> <td><code>ooxx</code></td>  <td> 0 </td> </tr>
 * <tr> <td><code>oxxo</code></td>  <td> 1 </td> </tr>
 * <tr> <td><code>xxoo</code></td>  <td> 2 </td> </tr>
 * </table>
 */
public class SplitL4w2s1e1b extends ImplementHashFunction {

  private static final int MASK_2_2 = 0xC;
  private static final int MASK_2_1 = 0x6;
  private static final long MASK_2_0 = 3L;

  private static final int NUMBER_BITS = 4;

  private static final int NUMBER_WINDOWS = 3;

  /**
   * A factory for the outer class.
   */
  public static final HashFunctionFactory FACTORY = new HashFunctionFactory() {
    @Override
    public NgsHashFunction create(final ReadCall readCall, final TemplateCall templateCall) {
      return new SplitL4w2s1e1b(readCall, templateCall);
    }

    @Override
    public int hashBits() {
      return NUMBER_BITS;
    }

    @Override
    public int windowBits() {
      return NUMBER_BITS;
    }

    @Override
    public int numberWindows() {
      return NUMBER_WINDOWS;
    }

    @Override
    public int windowSize() {
      return 2;
    }
  };

  /**
   * @param readCall used in subclasses to process results of read hits.
   * @param templateCall used in subclasses to process results of template hits.
   */
  public SplitL4w2s1e1b(final ReadCall readCall, final TemplateCall templateCall) {
    super(4, 2, readCall, templateCall);
    setHashFunction();
  }

  @Override
  public void readAll(final int readId, long v0, long v1) {
    if (mSoFar < 4) {
      return;
    }

    final long la = (v0 & MASK_2_0) << 2;
    final long lb = v1 & MASK_2_0;
    final long l0 = la | lb;
    mReadCall.readCall(readId, hash(l0), 0);

    final long lc = (v0 & MASK_2_1) << 1;
    final long ld = (v1 & MASK_2_1) >> 1;
    final long l1 = lc | ld;
    mReadCall.readCall(readId, hash(l1), 1);

    final long le = v0 & MASK_2_2;
    final long lf = (v1 & MASK_2_2) >> 2;
    final long l2 = le | lf;
    mReadCall.readCall(readId, hash(l2), 2);
  }

  @Override
  public void templateAll(final int endPosition, long v0, long v1) throws IOException {
    if (mSoFar < 4) {
      return;
    }

    final long la = (v0 & MASK_2_0) << 2;
    final long lb = v1 & MASK_2_0;
    final long l0 = la | lb;
    mTemplateCall.templateCall(endPosition, hash(l0), 0);

    final long lc = (v0 & MASK_2_1) << 1;
    final long ld = (v1 & MASK_2_1) >> 1;
    final long l1 = lc | ld;
    mTemplateCall.templateCall(endPosition, hash(l1), 1);

    final long le = v0 & MASK_2_2;
    final long lf = (v1 & MASK_2_2) >> 2;
    final long l2 = le | lf;
    mTemplateCall.templateCall(endPosition, hash(l2), 2);

    mTemplateCall.done();
  }

  @Override
  public int numberWindows() {
    return NUMBER_WINDOWS;
  }

  @Override
  public void toString(final StringBuilder sb) {
    sb.append("Split l=").append(readLength()).append(" w=").append(windowSize()).append(" no indels");
  }
}

