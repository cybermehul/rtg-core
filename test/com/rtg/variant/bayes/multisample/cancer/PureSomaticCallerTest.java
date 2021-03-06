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

package com.rtg.variant.bayes.multisample.cancer;


import static com.rtg.util.StringUtils.LS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.rtg.util.InvalidParamsException;
import com.rtg.variant.GenomePriorParams;
import com.rtg.variant.Variant;
import com.rtg.variant.VariantOutputLevel;
import com.rtg.variant.VariantParams;
import com.rtg.variant.VariantParamsBuilder;
import com.rtg.variant.bayes.Description;
import com.rtg.variant.bayes.Evidence;
import com.rtg.variant.bayes.Hypotheses;
import com.rtg.variant.bayes.Model;
import com.rtg.variant.bayes.ModelInterface;
import com.rtg.variant.bayes.multisample.HaploidDiploidHypotheses;
import com.rtg.variant.bayes.snp.EvidenceQ;
import com.rtg.variant.bayes.snp.HypothesesNone;
import com.rtg.variant.bayes.snp.HypothesesPrior;
import com.rtg.variant.bayes.snp.HypothesesSnp;
import com.rtg.variant.bayes.snp.StatisticsSnp;
import com.rtg.variant.dna.DNARange;
import com.rtg.variant.dna.DNARangeAT;
import com.rtg.variant.format.VariantOutputVcfFormatter;
import com.rtg.variant.util.arithmetic.SimplePossibility;

/**
 * This just contains a few extra tests that are specific to <code>CancerComparisonSimple</code>.
 * They may be subsumed by the superclass tests.
 *
 */
public class PureSomaticCallerTest extends AbstractSomaticCallerTest<Description> {

  @Override
  protected List<ModelInterface<Description>> getModel() {
    return getNormalModel();
  }

  @Override
  protected AbstractSomaticCaller getSomaticCaller(final double mutation, final Hypotheses<Description> hypotheses, String normalName, String cancerName, VariantParams params) {
    return new PureSomaticCaller(CombinedPriorsSnp.makeQ(mutation, 0.0, hypotheses), CombinedPriorsSnp.makeQ(mutation, 0.0, hypotheses), normalName, cancerName, params);
  }

  @Override
  protected Hypotheses<Description> getCancerHypotheses(double same, int ref) {
    return simpleHomoHyps(same, ref);
  }

  /** The result of 3 A reads, when reference is also A. */
  static final List<ModelInterface<Description>> EQUALS_REF_A =
      new PureSomaticCallerTest().doReads(3, DNARangeAT.A);

  /** The result of 3 C reads, when reference is A. */
  static final List<ModelInterface<Description>> SEEN_3_C =
      new PureSomaticCallerTest().doReads(3, DNARangeAT.C);

  /** The result of 3 G reads, when reference is A. */
  static final List<ModelInterface<Description>> SEEN_3_G =
      new PureSomaticCallerTest().doReads(3, DNARangeAT.G);

  protected static final String EXPECT_IDENTICAL = "chr1 14 . G A . PASS LOH=0.0;RSS=2.0;NCS=20.0;DP=2 GT:DP:RE:AR:GQ:ABP:SBP:RPB:PUR:RS:AD 1:1:0.020:0.000:21:0.00:2.17:0.00:0.00:A,1,0.020:0,1 1:1:0.020:0.000:25:0.00:2.17:0.00:0.00:A,1,0.020:0,1\n".replaceAll(" ", "\t");

  /**
   * Test that two identical SNP calls are not viewed as cancer.
   * @throws InvalidParamsException
   * @throws IOException
   */
  public void testIdentical() throws InvalidParamsException, IOException {
    final byte refNt = DNARange.G;
    final int refCode = refNt - 1;
    final HypothesesPrior<Description> hypotheses = simpleHomoHyps(0.7, refCode);
    final ModelInterface<?> model0 = new Model<>(hypotheses, new StatisticsSnp(hypotheses.description()));
    final ModelInterface<?> model1 = new Model<>(hypotheses, new StatisticsSnp(hypotheses.description()));
    final byte[] ref = new byte[14];
    ref[12] = DNARange.T;
    ref[13] = refNt;
    final Evidence ev = new EvidenceQ(hypotheses.description(), 0, 0, 0, 0.01, 0.01, true, false, false, false);
    model0.increment(ev);
    model1.increment(ev);
    final VariantParams params = new VariantParamsBuilder().callLevel(VariantOutputLevel.ALL).create();
    final AbstractSomaticCaller ccs = getSomaticCaller(0.3, simpleHomoHyps(0.7, refCode), "A", "C", params);
    ccs.integrity();
    //    final MultivarianceCall out = new MultivarianceCall("chr1", 13, 14, VarianceCallType.SNP);
    //    out.setName("G:A");


    final List<ModelInterface<?>> models = new ArrayList<>();
    models.add(model0);
    models.add(model1);
    final Variant v = ccs.makeCall("chr1", 13, 14, ref, models, new HaploidDiploidHypotheses<>(HypothesesNone.SINGLETON, hypotheses, null));
    final VariantOutputVcfFormatter formatter = getFormatter();
    assertEquals(EXPECT_IDENTICAL, formatter.formatCall(v));
  }

  protected static final String EXPECT_CANCER1 = "chr1 14 . G A,C . PASS SOMATIC=C;LOH=-1.0;RSS=29.3;NCS=293.4;DP=30 GT:DP:RE:AR:GQ:ABP:SBP:RPB:PUR:RS:AD 1:20:0.040:0.000:617:0.00:43.43:0.00:0.00:A,20,0.040:0,20,0 2:10:0.020:0.000:293:0.00:21.71:0.00:0.00:C,10,0.020:0,0,10\n".replaceAll(" ", "\t");

  /**
   * Test that a successful cancer call is made.
   * @throws InvalidParamsException
   * @throws IOException
   */
  public void testCancer1() throws InvalidParamsException, IOException {
    final byte refNt = DNARange.G;
    final int refCode = refNt - 1;
    final HypothesesPrior<Description> hypotheses = simpleHomoHyps(0.7, refCode);
    final ModelInterface<?> model0 = new Model<>(hypotheses, new StatisticsSnp(hypotheses.description()));
    final ModelInterface<?> model1 = new Model<>(hypotheses, new StatisticsSnp(hypotheses.description()));
    final byte[] ref = new byte[14];
    ref[12] = DNARange.T;
    ref[13] = refNt;

    final Evidence eva = new EvidenceQ(hypotheses.description(), 0, 0, 0, 0.001, 0.001, true, false, false, false);
    final Evidence evc = new EvidenceQ(hypotheses.description(), 1, 0, 0, 0.001, 0.001, true, false, false, false);

    for (int coverage = 0; coverage < 10; coverage++) {
      model0.increment(eva);
      model0.increment(eva);
      model1.increment(evc);
    }
    final VariantParams params = new VariantParamsBuilder().callLevel(VariantOutputLevel.ALL).create();
    final AbstractSomaticCaller ccs = getSomaticCaller(0.003, hypotheses, "A", "C", params);
    ccs.integrity();
    //    final MultivarianceCall out = new MultivarianceCall("chr1", 13, 14, VarianceCallType.SNP);
    //    out.setName("G:A");

    final List<ModelInterface<?>> models = new ArrayList<>();
    models.add(model0);
    models.add(model1);
    final Variant v = ccs.makeCall("chr1", 13, 14, ref, models, new HaploidDiploidHypotheses<>(HypothesesNone.SINGLETON, hypotheses, null));
    final VariantOutputVcfFormatter formatter = getFormatter();
    assertEquals(EXPECT_CANCER1, formatter.formatCall(v));
  }

  public void testQ() {
    final GenomePriorParams params = GenomePriorParams.builder().create();
    final VariantParams vParams = VariantParams.builder().genomePriors(params).create();
    //final AbstractCancerComparison ccs = getCancerComparison(0.3, DiploidSnpBayesian.getCategories(params), null, null);
    final AbstractSomaticCaller ccs = getSomaticCaller(0.3, new HypothesesSnp(SimplePossibility.SINGLETON, params, false, 0), null, null, vParams);
    ccs.integrity();
    final String exp = ""
        + "length=10" + LS
        //   A     C     G     T   A:C   A:G   A:T   C:G   C:T   G:T
        + "0.490 0.010 0.010 0.010 0.140 0.020 0.020 0.140 0.020 0.140 " + LS
        + "0.010 0.490 0.010 0.010 0.140 0.140 0.020 0.020 0.140 0.020 " + LS
        + "0.010 0.010 0.490 0.010 0.020 0.140 0.140 0.140 0.020 0.020 " + LS
        + "0.010 0.010 0.010 0.490 0.020 0.020 0.140 0.020 0.140 0.140 " + LS
        + "0.070 0.070 0.010 0.010 0.500 0.080 0.020 0.080 0.080 0.080 " + LS
        + "0.010 0.070 0.070 0.010 0.080 0.500 0.080 0.080 0.080 0.020 " + LS
        + "0.010 0.010 0.070 0.070 0.020 0.080 0.500 0.080 0.080 0.080 " + LS
        + "0.070 0.010 0.070 0.010 0.080 0.080 0.080 0.500 0.020 0.080 " + LS
        + "0.010 0.070 0.010 0.070 0.080 0.080 0.080 0.020 0.500 0.080 " + LS
        + "0.070 0.010 0.010 0.070 0.080 0.020 0.080 0.080 0.080 0.500 " + LS
        ;
    assertEquals(exp, ccs.toString());
  }

}
