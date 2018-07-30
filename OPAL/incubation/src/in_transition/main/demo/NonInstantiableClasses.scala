/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL

import org.opalj.br.analyses.cg.InstantiableClassesKey
import org.opalj.util.Nanoseconds
import org.opalj.util.asMB
import org.opalj.util.PerformanceEvaluation.{memory, time}

/**
 * Basic information about (non-)instantiable classes.
 *
 * ==Example Usage==
 * {{{
 * run
 * }}}
 *
 * @author Michael Eichberg
 */
object NonInstantiableClasses extends DefaultOneStepAnalysis {

    override def description: String = "Provides information about instantiable classes."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        var overallExecutionTime = Nanoseconds.None
        var memoryUsageInMB = ""

        val instantiableClasses =
            memory {
                time {
                    project.get(InstantiableClassesKey)
                } { t ⇒ overallExecutionTime += t }
            } { memoryUsage ⇒ memoryUsageInMB = asMB(memoryUsage) }

        val notInstantiableClasse = instantiableClasses.notInstantiable.map { ot ⇒
            val methods = project.classFile(ot).get.methods
            val instanceMethods = methods.filter { m ⇒ !m.isStatic && !m.isConstructor }
            ot.toJava+"("+instanceMethods.size+")"
        }
        val sortedNotInstantiableClasse = notInstantiableClasse.toSeq.sorted

        BasicReport(
            instantiableClasses.statistics.mkString(
                "determing non-instantiable classes "+
                    "took "+overallExecutionTime.toSeconds+" "+
                    "and required "+memoryUsageInMB+":\n",
                "\n",
                "\n"
            ) +
                sortedNotInstantiableClasse.mkString("List of all non-instantiable classes:\n\t", "\n\t", "\n")
        )
    }
}
