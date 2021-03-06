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
package com.rtg;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Entry point for RTG tools
 */
public final class ToolsEntry extends AbstractCliEntry {

  /**
   * @param args command line arguments
   */
  public static void main(String[] args) {
    new ToolsEntry().mainImpl(args);
  }

  @Override
  protected Command getSlimModule(String arg) {
    return ToolsCommand.INFO.findModuleWithExpansion(arg);
  }

  @Override
  protected int help(String[] shiftArgs, OutputStream out, PrintStream err) {
    return ToolsCommand.HELP.module().mainInit(shiftArgs, out, err);
  }
}
