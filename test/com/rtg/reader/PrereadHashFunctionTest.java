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
package com.rtg.reader;


import junit.framework.TestCase;

/**
 * Tests.
 */
public class PrereadHashFunctionTest extends TestCase {

  public PrereadHashFunctionTest(final String name) {
    super(name);
  }

  public void testInit() {
    PrereadHashFunction prf = new PrereadHashFunction();
    assertEquals(0L, prf.getHash());
  }

  public void testInt() {
    PrereadHashFunction prf = new PrereadHashFunction();
    for (int i = 0 ; i < 1000; i++) {
      prf.irvineHash(i);
    }
    assertEquals(8815098401637430148L, prf.getHash());
  }

  public void testLong() {
    PrereadHashFunction prf = new PrereadHashFunction();
    for (long l = 0 ; l < 1000; l++) {
      prf.irvineHash(l);
    }
    assertEquals(6022146393827695201L, prf.getHash());
  }

  public void testOutput() {
    PrereadHashFunction prf = new PrereadHashFunction();
    prf.irvineHash(5);
    prf.irvineHash("PrereadHashFunction");
    assertEquals(-139924295097060585L, prf.getHash());
  }

  public void testString() {
    PrereadHashFunction prf = new PrereadHashFunction();
    prf.irvineHash("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    assertEquals(-159429765726103168L, prf.getHash());
  }

  private static final long[] HASH = {
    -4964420948893066024L,
    7564655870752979346L,
    3831662765844904176L,
    6137546356583794141L,
    -594798593157429144L,
    112842269129291794L,
    -669528114487223426L,
    -1109287713991315740L,
    -974081879987450628L,
    -1160629452687687109L,
    7326573195622447256L,
    6410576364588137014L,
    5424394867226112926L,
    -9103770306483490189L,
    2139215297105423308L,
    -4232865876030345843L,
    -6273872167485304708L,
    2891469594365336806L,
    6976596177944619528L,
    2578166436595196069L,
    -5627216606837767319L,
    -3592913410653813758L,
    92698085241473569L,
    -8796603504740353600L,
    -4722652817683412901L,
    2619856624980352251L,
    8886318912347348303L,
    -8401480976436315613L,
    -7801123389691242517L,
    3779987867844568136L,
    -6947711303906420817L,
    3407244680303549079L,
    197092594700490712L,
    2970725011242582564L,
    3284532136690698432L,
    -8478177725643278359L,
    -482677293272704124L,
    4527320925905780494L,
    7277626163180921831L,
    4014050679668805482L,
    7969120158891947125L,
    4300965142756182089L,
    -2030825140507191061L,
    707006413279611759L,
    -7519275600551226667L,
    -6360924135797636003L,
    2210640064016022649L,
    -6410673298797731886L,
    -289193436830779917L,
    3813634057487595412L,
    6911063436971917473L,
    8547294963617019503L,
    6154022364946197696L,
    8175826803456981118L,
    -9147084144055124649L,
    -18800628192384088L,
    -6817826759444601261L,
    -1667880028869348243L,
    -9082071447080613645L,
    9065674809775364834L,
    7909671975457870438L,
    5683311091615826937L,
    -5214481407826501455L,
    -693328208225879290L,
    3864458965704708566L,
    3184808690151788414L,
    -8320357513071910606L,
    -8200160751728555263L,
    -7603456060161050842L,
    -3888746125786119271L,
    -5552347832537805063L,
    3774859742041214532L,
    4702249276633814781L,
    -4096719924219374161L,
    4150930343758163695L,
    -311691390498039484L,
    -3622597253628401501L,
    -3019456038419834778L,
    3008729024856518368L,
    -6686992125460025861L,
    161601140914943624L,
    -6803345800057020374L,
    3836516331752709628L,
    -2207018395996362651L,
    -5404080405594186050L,
    -5102892484113533015L,
    -9048258330105186985L,
    -237923595412718844L,
    2826893961496978298L,
    -5338953178777934760L,
    -3246979880425410455L,
    2281331448982092637L,
    -7065999876450923625L,
    8888791547312749291L,
    1840067945267344782L,
    -7062411921403462023L,
    518729297779020562L,
    -7536618281581788192L,
    1347092278477147782L,
    1365943130551420261L,
    -5904149397109527665L,
    5165118076730241013L,
    -7305211479695003402L,
    -2773637612724504142L,
    6526887576802954450L,
    -7403923644694799186L,
    5388172503113520870L,
    -2279230739761038859L,
    -4717761859960318649L,
    7807265917042125009L,
    6932437597733693250L,
    5004478446554740296L,
    -4983868948686226820L,
    -2089196626022557463L,
    806172501569318489L,
    8443078202631527623L,
    -2537354127574070879L,
    -1809183693800895546L,
    1152708571114219105L,
    -4356742874647865835L,
    7889674025587210255L,
    -15063047445053702L,
    1141886611049844721L,
    -7631037532535991852L,
    -7982034127000075330L,
    -6234520482433768610L,
    -1710360246199412092L,
    4546857235350971184L,
    4583808669371655117L,
    -5407509412797283957L,
    6229851483527453949L,
    -7243389174711685803L,
    5818523204758407422L,
    451431109683129954L,
    8319638437045870110L,
    6809326219677622260L,
    8556296580499353143L,
    3269551181474795397L,
    -5974473391241630449L,
    -5246761733127519293L,
    -4733994914873378558L,
    -1307825960948043813L,
    -7565129111504795170L,
    -6566813981172607238L,
    2038177896599595533L,
    -4157820461140106224L,
    5653609452294381049L,
    -2202565841775117866L,
    -6817890117611987541L,
    -1311679443604766958L,
    3628279229051225269L,
    -4525977720206120167L,
    2907771609439525970L,
    778059278289524373L,
    6984359371816035275L,
    -3936364606998129528L,
    -5298787210079405285L,
    2034188968277120912L,
    -4387870378401475627L,
    554672037112194924L,
    5840819797252213829L,
    -6141412821020480834L,
    7866485694190557398L,
    7388975574969141522L,
    -4726765176532574285L,
    6738701484706097438L,
    -4357952859750176081L,
    5952195970042072907L,
    -8988751357500304535L,
    8830011414065963124L,
    2419637729810828715L,
    586541553579625708L,
    8198777404514432018L,
    -5690429332067571771L,
    -5182139003232109836L,
    -4096094371451621809L,
    5314520057811676541L,
    -4033293396040907809L,
    -622504768958473957L,
    -64131069627898415L,
    4410735263197203227L,
    8212144607526460367L,
    2402170585793741972L,
    4325283475773974610L,
    -6344159268077467517L,
    -7890971429993775768L,
    -8190969857040160713L,
    4617151108230248888L,
    -1470552416272715281L,
    -5095890738279829053L,
    -4113790345307396671L,
    4469205987600845164L,
    3241129027203716284L,
    8205001152228352441L,
    -3141642311557935535L,
    -7343378748249317669L,
    -6678700892246573074L,
    5596136011188865643L,
    1206361913488075499L,
    -1143135553327786506L,
    4914520655968408745L,
    4227465294407942587L,
    5607375553831556344L,
    6861091380696820658L,
    -2635540877404264775L,
    -3558812817526879536L,
    7170789582926847684L,
    4892933472165288585L,
    597173744976948491L,
    -6091386647335781738L,
    1371725377195789311L,
    -7210074501640255752L,
    3295738803696009778L,
    546636298196020284L,
    -5383611729103365571L,
    5124104824591383551L,
    2135919692763215075L,
    -5695875964136749644L,
    6438111700768233846L,
    4208357349436110756L,
    3091674171583948921L,
    -7477735970847863754L,
    -3506913595212098426L,
    -8947977927743584400L,
    6437570226861442418L,
    979401604932372669L,
    -7264508646647264850L,
    -5874799961348262672L,
    -7678580529857168913L,
    6820729910234791048L,
    -996271069855517935L,
    -5784334880520928715L,
    4142656736146180664L,
    8201412039385703835L,
    -5393578045340737736L,
    549670855068890709L,
    -6292301921292308170L,
    5827649394587063099L,
    -3398439017277695601L,
    2869919216733092328L,
    8363831910784389368L,
    5897574581860534902L,
    -8306706606536940827L,
    -8812761396236860533L,
    5816535479574000431L,
    -5605727922123064679L,
    1750179170939337200L,
    -759857275904125856L,
    2392129137028815281L,
    435317679251669431L,
    1562823400580920263L,
    965148254923310220L,
    8669113822474706681L,
    -3326272830183554775L,
    -570055038919252890L,
    6456096406736068536L,
  };

  public void testHashes() {
    assertEquals(256, PrereadHashFunction.HASH_BLOCKS.length);
    for (int i = 0; i < HASH.length; i++) {
      assertEquals(HASH[i], PrereadHashFunction.HASH_BLOCKS[i]);
    }

  }
}