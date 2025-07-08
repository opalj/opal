/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.cli.FieldNameArg
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.util.PerformanceEvaluation.memory
import org.opalj.util.asMB

/**
 * Basic field access information.
 *
 * ==Example Usage==
 * {{{
 * run -cp=/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/ -field=java.util.HashMap.entrySet
 * }}}
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationAnalysis extends ProjectsAnalysisApplication {

    protected class FieldAccessConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Provides information about field accesses"

        args(FieldNameArg)
    }

    protected type ConfigType = FieldAccessConfig

    protected def createConfig(args: Array[String]): FieldAccessConfig = new FieldAccessConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: FieldAccessConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        implicit val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)

        var memoryUsage = ""
        val accessInformation = memory {
            project.get(FieldAccessInformationKey)
        } { m => memoryUsage = asMB(m) }

        val report =
            if (analysisConfig.get(FieldNameArg).isDefined) {
                val results = new StringBuilder()
                for { (declaringClassName, fieldName) <- analysisConfig(FieldNameArg) } {
                    val declaringClassType = ClassType(declaringClassName)
                    val fields = project.classFile(declaringClassType).map(_.findField(fieldName)).getOrElse(List.empty)

                    val (reads, writes) = fields
                        .foldLeft((Seq.empty[(DefinedMethod, PCs)], Seq.empty[(DefinedMethod, PCs)])) {
                            (accesses, field) =>
                                val newReads =
                                    (accesses._1 ++ accessInformation.readAccesses(field))
                                        .asInstanceOf[Seq[(DefinedMethod, PCs)]]
                                val newWrites =
                                    (accesses._2 ++ accessInformation.writeAccesses(field))
                                        .asInstanceOf[Seq[(DefinedMethod, PCs)]]

                                (newReads, newWrites)
                        }

                    def accessInformationToString(data: Seq[(DefinedMethod, PCs)]): String = {
                        data.map { e =>
                            val (method, pcs) = e
                            method.definedMethod.toJava(pcs.mkString("pcs: ", ", ", ""))
                        }.mkString("\t ", "\n\t ", "\n")
                    }

                    results.append(
                        declaringClassName + " " + fieldName + "\n" +
                            "writes:\n" + accessInformationToString(writes) +
                            "reads:\n" + accessInformationToString(reads) + "\n"
                    )
                }
                BasicReport(results.toString())
            } else {
                BasicReport(
                    accessInformation.statistics
                        .mkString(s"determing field access information required $memoryUsage :\n", "\n", "\n")
                )
            }

        (project, report)
    }
}
