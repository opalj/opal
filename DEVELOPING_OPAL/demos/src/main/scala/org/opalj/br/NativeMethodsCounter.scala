/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project

/**
 * Counts the number of native methods.
 *
 * @author Michael Eichberg
 */
object NativeMethodsCounter extends ProjectAnalysisApplication {

    override def description: String = "Counts the number of native methods."

    def doAnalyze(p: Project[URL], params: Seq[String], isInterrupted: () => Boolean): BasicReport = {
        val nativeMethods = p.allClassFiles.flatMap(_.methods.filter(_.isNative).map(_.toJava))
        BasicReport(
            nativeMethods.mkString(s"${nativeMethods.size} native methods found:\n\t", "\n\t", "\n")
        )
    }
}
