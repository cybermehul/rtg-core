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

package com.rtg.variant.sv.discord;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;

import com.rtg.util.ByteUtils;
import com.rtg.util.CompareHelper;
import com.rtg.util.ReorderingQueue;
import com.rtg.util.diagnostic.Diagnostic;
import com.rtg.vcf.VcfRecord;
import com.rtg.vcf.header.VcfHeader;


/**
 * Class to reorder VCF records during output. Note that only one record per position is supported.
 */
class SmartVcfWriter extends ReorderingQueue<VcfRecord> {

  private static final int BUFFER_SIZE = 10000; // Assume records will be out of order by at most this amount.

  private static final class VcfPositionalComparator implements Comparator<VcfRecord>, Serializable {
    @Override
    public int compare(VcfRecord o1, VcfRecord o2) {
      return new CompareHelper()
          .compare(o1.getSequenceName(), o2.getSequenceName())
          .compare(o1.getStart(), o2.getStart())
          .compareList(o1.getAltCalls(), o2.getAltCalls())
          .compare(System.identityHashCode(o1), System.identityHashCode(o2))
          .result();
    }
  }

  final OutputStream mOut;

  SmartVcfWriter(OutputStream out, VcfHeader header) throws IOException {
    super(BUFFER_SIZE, new VcfPositionalComparator());
    mOut = out;
    mOut.write(header.toString().getBytes());
  }


  @Override
  protected String getReferenceName(VcfRecord record) {
    return record.getSequenceName();
  }

  @Override
  protected int getPosition(VcfRecord record) {
    return record.getStart();
  }

  @Override
  protected void flushRecord(VcfRecord rec) throws IOException {
    mOut.write(rec.toString().getBytes());
    ByteUtils.writeNewline(mOut);
  }

  @Override
  protected void reportReorderingFailure(VcfRecord rec) {
    Diagnostic.warning("VcfRecord dropped due to excessive out-of-order processing.\n" + rec.toString());
  }

  @Override
  public void close() throws IOException {
    super.close();
    mOut.close();
  }
}
