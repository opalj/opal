/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.fpcf.xltest
import org.opalj.br.fpcf.FPCFAnalysisScheduler

/**
 * Run XL Tests, but with TAJS analyses disabled
 */
class XLNoJavaScriptBenchmark extends XLJavaScriptTests {
  override def addAnalyses(): Iterable[FPCFAnalysisScheduler] = {List()}
}
