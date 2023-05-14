/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.Field
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerFieldAccessInformationAnalysis
import org.opalj.br.fpcf.properties.FieldAccessInformation
import org.opalj.br.fpcf.properties.NoFieldAccessInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import java.net.URL

/**
 * Runs the EagerFieldAccessInformationAnalysis.
 *
 * @author Maximilian Rüsch
 */
object FieldAccessInformationAnalysisDemo extends ProjectAnalysisApplication {

    override def title: String = "Determines read and write accesses to fields"

    override def description: String = "Identifies fields which are never written nor read"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {
        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        project.get(RTACallGraphKey)

        time {
            propertyStore = analysesManager
                .runAll(EagerFieldAccessInformationAnalysis)
                ._1
            propertyStore.waitOnPhaseCompletion()
        } { t =>
            analysisTime = t.toSeconds
        }

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap {
            _.fields
        }.toSet

        val groupedResults = propertyStore
            .entities(FieldAccessInformation.key)
            .filter(ep => allFieldsInProjectClassFiles.contains(ep.e.asInstanceOf[Field]))
            .iterator
            .to(Iterable)
            .groupBy(property => {
                property.asFinal.p match {
                    case NoFieldAccessInformation => "none"
                    case info: FieldAccessInformation =>
                        if (info.readAccesses.nonEmpty && info.writeAccesses.isEmpty)
                            "readAccesses"
                        else if (info.readAccesses.isEmpty && info.writeAccesses.nonEmpty)
                            "writeAccesses"
                        else
                            "both"
                }
            })

        val order = (eps1: EPS[Entity, FieldAccessInformation], eps2: EPS[Entity, FieldAccessInformation]) =>
            eps1.e.toString < eps2.e.toString
        val readFields = groupedResults.getOrElse("readAccesses", Set()).toSeq.sortWith(order)
        val writtenFields = groupedResults.getOrElse("writeAccesses", Set()).toSeq.sortWith(order)
        val readAndWrittenFields = groupedResults.getOrElse("both", Set()).toSeq.sortWith(order)

        s"""
           |
           | Not Accessed Fields: ${project.projectFieldsCount - readFields.size - writtenFields.size - readAndWrittenFields.size}
           | Purely Read Fields : ${readFields.size}
           | Purely Written Fields: ${writtenFields.size}
           | Read And Written Fields: ${readAndWrittenFields.size}
           |
           | total Fields: ${project.projectFieldsCount}
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
