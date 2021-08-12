/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.util.asMB
import org.opalj.util.PerformanceEvaluation.memory

import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.FieldAccessInformationKey

/**
 * Basic field access information.
 *
 * ==Example Usage==
 * {{{
 * run -cp=/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/ -field="java.util.HashMap entrySet"
 * }}}
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationAnalysis extends ProjectAnalysisApplication {

    override def description: String = "provides information about field accesses"

    override def analysisSpecificParametersDescription: String = {
        "[-field=\"<The field for which we want read/write access information "+
            "(e.g., -field=\"java.util.HashMap entrySet\">\"]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        if (parameters.isEmpty || (parameters.size == 1 && parameters.head.startsWith("-field="))) {
            Seq.empty
        } else {
            Seq("unknown parameters: "+parameters.mkString(" "))
        }
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        var memoryUsage = ""
        val accessInformation = memory {
            project.get(FieldAccessInformationKey)
        } { m => memoryUsage = asMB(m) }

        if (parameters.nonEmpty) {
            val Array(declaringClassName, fieldName) =
                parameters.head.substring(7).replace('.', '/').split(' ')
            val declaringClassType = ObjectType(declaringClassName)
            val writes = accessInformation.writeAccesses(declaringClassType, fieldName)
            val reads = accessInformation.readAccesses(declaringClassType, fieldName)

            def accessInformationToString(data: Seq[(Method, PCs)]): String = {
                (
                    data.map { e =>
                        val (method, pcs) = e
                        method.toJava(pcs.mkString("pcs: ", ", ", ""))
                    }
                ).mkString("\t ", "\n\t ", "\n")
            }

            BasicReport(
                declaringClassName+" "+fieldName+"\n"+
                    "writes:\n"+accessInformationToString(writes)+
                    "reads:\n"+accessInformationToString(reads)
            )

        } else {
            BasicReport(
                accessInformation.statistics.mkString(
                    s"determing field access information required $memoryUsage :\n", "\n", "\n"
                )
            )
        }
    }
}
