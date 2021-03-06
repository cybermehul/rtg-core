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

package com.rtg.visualization;

import com.rtg.sam.SamUtils;
import com.rtg.util.StringUtils;

import net.sf.samtools.SAMRecord;

/**
 */
public class SamAssistanceSimple implements SamAssistance {

  private static final String INVALID_CIGAR = "Invalid cigar : ";

  @Override
  public String[] samToReads(final SAMRecord sam, final String template, byte[] templateBytes, final int readStart, final boolean displayDots) {
    final String read = sam.getReadString();
    final String cigar = sam.getCigarString();
    return new String[] {cigarToReads(cigar, read, template, readStart, displayDots)};
  }

  /**
   * @param cigar string.
   * @param read nucleotides of read.
   * @param readStart where the read starts on the template (in screen co-ordinates).
   * @param displayDots if lines match display dot
   * @return return a string with a display of the current read.
   */
  String cigarToReads(String cigar, String read, String template, int readStart, boolean displayDots) {
    //System.err.println("cigar=" + cigar + " read=" + read + " template=" + template + " readStart=" + readStart);
    final StringBuilder sb = new StringBuilder();
    int n = 0;
    int rPos = 0;
    //beware tPos is in screen co-ordinates
    int tPos = readStart;
    //assert template.charAt(tPos) != '_';
    boolean isFirstAction = true; //this is so we can deal properly with inserts at the start of reads... although it's sure to be broken for reads that start with inserts that aren't at the start of the template.
    for (int i = 0; i < cigar.length(); i++) {
      final char c = cigar.charAt(i);
      if (Character.isDigit(c)) {
        n = 10 * n + c - '0';
      } else {
        assert n > 0 : cigar;
        //System.err.println("i=" + i + " n=" + n + " c=" + c);
        for (int j = 0; j < n; ) {
          if (tPos >= template.length()) {
            break;  //oh shiiiiiii
          }
          //System.err.println("  j=" + j + " rPos=" + rPos + " tPos=" + tPos + " te=" + template.charAt(tPos) + " sb=" + sb.toString());
          if (c == SamUtils.CIGAR_INSERTION_INTO_REF) {
            if (!isFirstAction) {
              tPos += n;
            } else {
              sb.append(StringUtils.getSpaceString(readStart - n));
              isFirstAction = false;
            }
            // assert template.charAt(tPos) == '_' : tPos;
            for (int k = 0; k < n; k++) {
              sb.append(read.charAt(rPos + k));
            }
            rPos += n;

            break;  //process ALL the inserts at the same time, so break out of this entire loop...
          }
          if (template.charAt(tPos) == '_') {
            sb.append("_");
            tPos++;
            continue;
          }
          j++;

          switch (c) {
            case SamUtils.CIGAR_SAME:
            case SamUtils.CIGAR_SAME_OR_MISMATCH:
            case SamUtils.CIGAR_MISMATCH:
              if (isFirstAction) {
                sb.append(StringUtils.getSpaceString(readStart));
                isFirstAction = false;
              }
              final char readChar = Character.toLowerCase(read.charAt(rPos));
              final char templateChar = Character.toLowerCase(template.charAt(tPos));
              if (readChar == templateChar && displayDots) {
                if (readChar == 'n') {
                  sb.append('N');
                } else {
                  sb.append('.');
                }
              } else {
                sb.append(read.charAt(rPos));
              }
              rPos++;
              tPos++;
              break;
            case SamUtils.CIGAR_DELETION_FROM_REF:
              if (isFirstAction) {
                sb.append(StringUtils.getSpaceString(readStart));
                isFirstAction = false;
              }
              sb.append("-");
              tPos++;
              break;
            case SamUtils.CIGAR_GAP_IN_READ:
              if (isFirstAction) {
                sb.append(StringUtils.getSpaceString(readStart));
                isFirstAction = false;
              }
              sb.append(" ");
              tPos++;
              break;
            case SamUtils.CIGAR_SOFT_CLIP:
              rPos++;
              //soft clip doesn't really count as a first action!
              break;
            case SamUtils.CIGAR_HARD_CLIP:
              break;
            default:
              throw new IllegalStateException(INVALID_CIGAR + cigar);

          }
        }
        n = 0;
      }
    }
    return sb.toString();
  }
}
