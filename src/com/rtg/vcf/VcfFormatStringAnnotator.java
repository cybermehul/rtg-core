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

package com.rtg.vcf;

import com.rtg.vcf.annotation.AbstractDerivedFormatAnnotation;
import com.rtg.vcf.header.FormatField;
import com.rtg.vcf.header.MetaType;
import com.rtg.vcf.header.VcfHeader;
import com.rtg.vcf.header.VcfNumber;

/**
 */
public class VcfFormatStringAnnotator implements VcfAnnotator {

  final AbstractDerivedFormatAnnotation mAnnotation;

  /**
   * Create an FORMAT annotation that outputs a string value.
   * @param annotation the annotation to use.
   */
  public VcfFormatStringAnnotator(AbstractDerivedFormatAnnotation annotation) {
    assert annotation != null && annotation.getType().getClassType() == String.class;
    mAnnotation = annotation;
  }

  @Override
  public void updateHeader(VcfHeader header) {
    header.ensureContains(new FormatField(mAnnotation.getName(), MetaType.STRING, new VcfNumber("1"), mAnnotation.getDescription()));
  }

  @Override
  public void annotate(VcfRecord rec) {
    for (int i = 0; i < rec.getNumberOfSamples(); i++) {
      final String val = (String) mAnnotation.getValue(rec, i);
      if (val != null) {
        rec.setFormatAndSample(mAnnotation.getName(), val, i);
      }
    }
    rec.padFormatAndSample(mAnnotation.getName());
  }

}
