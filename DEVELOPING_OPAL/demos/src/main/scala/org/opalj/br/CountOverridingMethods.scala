/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport

/**
 * Counts the number of methods that override/implement an instance method.
 *
 * @author Michael Eichberg
 */
object CountOverridingMethods extends AnalysisApplication {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String = "Counts the number of methods that override a method."

        def doAnalyze(
            project:       Project[URL],
            parameters:    Seq[String],
            isInterrupted: () => Boolean
        ) = {
            val overridingMethodsInfo =
                project.overridingMethods.view.
                    map(ms => (ms._1, ms._2 - ms._1)).
                    filter(_._2.nonEmpty).map { ms =>
                        val (method, allOverridingMethods) = ms
                        val overridingMethods = allOverridingMethods.map(m => m.classFile.fqn)
                        (method, (overridingMethods, overridingMethods.size))
                        val count = overridingMethods.size
                        method.toJava(
                            overridingMethods.mkString(s"\n\thas $count overridde(s):\n\t\t", "\n\t\t", "\n")
                        )
                    }

            BasicReport(
                "Overriding\n"+overridingMethodsInfo.mkString("\n")
            )
        }
    }
}
