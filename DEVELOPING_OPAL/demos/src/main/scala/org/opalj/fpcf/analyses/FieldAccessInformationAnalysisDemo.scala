/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.analyses.EagerFieldAccessInformationAnalysis
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldWriteAccessInformation
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysisScheduler
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
                .runAll(
                    EagerFieldAccessInformationAnalysis,
                    ReflectionRelatedFieldAccessesAnalysisScheduler
                )
                ._1
            propertyStore.waitOnPhaseCompletion()
        } { t =>
            analysisTime = t.toSeconds
        }

        val allFieldsInProjectClassFiles = project.allProjectClassFiles.iterator.flatMap {
            _.fields
        }.toSet

        val readFields = propertyStore
            .entities(FieldReadAccessInformation.key)
            .filter(ep => allFieldsInProjectClassFiles.contains(ep.e.asInstanceOf[Field])
                && ep.asFinal.p != NoFieldReadAccessInformation)
            .map(_.e)
            .toSet
        val writtenFields = propertyStore
            .entities(FieldWriteAccessInformation.key)
            .filter(ep => allFieldsInProjectClassFiles.contains(ep.e.asInstanceOf[Field])
                && ep.asFinal.p != NoFieldWriteAccessInformation)
            .map(_.e)
            .toSet

        val readAndWrittenFields = readFields intersect writtenFields
        val purelyReadFields = readFields diff readAndWrittenFields
        val purelyWrittenFields = writtenFields diff readAndWrittenFields

        val totalIncompleteAccessSiteCount = propertyStore
            .entities(MethodFieldReadAccessInformation.key)
            .filter(ep => project.allMethodsWithBody.contains(ep.e.asInstanceOf[Method]))
            .map(_.asFinal.p.numIncompleteAccessSites)
            .sum

        s"""
           |
           | Not Accessed Fields: ${project.projectFieldsCount - purelyReadFields.size - purelyWrittenFields.size - readAndWrittenFields.size}
           | Purely Read Fields : ${purelyReadFields.size}
           | Purely Written Fields: ${purelyWrittenFields.size}
           | Read And Written Fields: ${readAndWrittenFields.size}
           |
           | Access Sites with missing information: $totalIncompleteAccessSiteCount
           |
           | total Fields: ${project.projectFieldsCount}
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
