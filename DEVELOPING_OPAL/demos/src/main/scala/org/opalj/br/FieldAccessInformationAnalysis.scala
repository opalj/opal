/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.util.PerformanceEvaluation.memory
import org.opalj.util.asMB

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
        "[-field=\"<The field for which we want read/write access information " +
            "(e.g., -field=\"java.util.HashMap entrySet\">\"]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {
        if (parameters.isEmpty || (parameters.size == 1 && parameters.head.startsWith("-field="))) {
            Seq.empty
        } else {
            Seq("unknown parameters: " + parameters.mkString(" "))
        }
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        implicit val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)

        var memoryUsage = ""
        val accessInformation = memory {
            project.get(FieldAccessInformationKey)
        } { m => memoryUsage = asMB(m) }

        if (parameters.nonEmpty) {
            val Array(declaringClassName, fieldName) =
                parameters.head.substring(7).replace('.', '/').split(' ')
            val declaringClassType = ObjectType(declaringClassName)
            val fields = project.classFile(declaringClassType).map(_.findField(fieldName)).getOrElse(List.empty)

            val (reads, writes) = fields
                .foldLeft((Seq.empty[(DefinedMethod, PCs)], Seq.empty[(DefinedMethod, PCs)])) { (accesses, field) =>
                    val newReads = (accesses._1 ++ accessInformation.readAccesses(field)).asInstanceOf[Seq[(DefinedMethod, PCs)]]
                    val newWrites = (accesses._2 ++ accessInformation.writeAccesses(field)).asInstanceOf[Seq[(DefinedMethod, PCs)]]

                    (newReads, newWrites)
                }

            def accessInformationToString(data: Seq[(DefinedMethod, PCs)]): String = {
                (
                    data.map { e =>
                        val (method, pcs) = e
                        method.definedMethod.toJava(pcs.mkString("pcs: ", ", ", ""))
                    }
                ).mkString("\t ", "\n\t ", "\n")
            }

            BasicReport(
                declaringClassName + " " + fieldName + "\n" +
                    "writes:\n" + accessInformationToString(writes) +
                    "reads:\n" + accessInformationToString(reads)
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
