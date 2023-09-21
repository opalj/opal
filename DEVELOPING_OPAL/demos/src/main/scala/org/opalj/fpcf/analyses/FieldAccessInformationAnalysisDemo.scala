/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
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
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

import java.net.URL

/**
 * Runs analyses for field accesses throughout a project and automatically excludes any JDK files included in the project
 * files from the summary at the end.
 *
 * @author Maximilian Rüsch
 */
object FieldAccessInformationAnalysisDemo extends ProjectAnalysisApplication {

    private val JDKPackages = List("java/", "javax", "javafx", "jdk", "sun", "oracle", "com/sun",
        "netscape", "org/ietf/jgss", "org/jcp/xml/dsig/internal", "org/omg", "org/w3c/dom",
        "org/xml/sax")

    override def title: String = "FieldAccessInformationAnalysis"

    override def description: String = "Runs analyses for field accesses (field reads and writes) throughout a project"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val result = analyze(project)
        BasicReport(result)
    }

    def analyze(project: Project[URL]): String = {
        val domain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        var propertyStore: PropertyStore = null
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        time {
            propertyStore = analysesManager
                .runAll(
                    EagerFieldAccessInformationAnalysis
                )
                ._1
            propertyStore.waitOnPhaseCompletion()
        } { t =>
            analysisTime = t.toSeconds
        }

        val projectClassFiles = project.allProjectClassFiles.iterator.filter { cf =>
            !JDKPackages.exists(cf.thisType.packageName.startsWith)
        }
        val fields = projectClassFiles.flatMap { _.fields }.toSet

        val readFields = propertyStore
            .entities(FieldReadAccessInformation.key)
            .filter(ep => fields.contains(ep.e.asInstanceOf[Field])
                && ep.asFinal.p != NoFieldReadAccessInformation)
            .map(_.e)
            .toSet
        val writtenFields = propertyStore
            .entities(FieldWriteAccessInformation.key)
            .filter(ep => fields.contains(ep.e.asInstanceOf[Field])
                && ep.asFinal.p != NoFieldWriteAccessInformation)
            .map(_.e)
            .toSet

        val readAndWrittenFields = readFields intersect writtenFields
        val purelyReadFields = readFields diff readAndWrittenFields
        val purelyWrittenFields = writtenFields diff readAndWrittenFields

        val totalIncompleteAccessSiteCount = propertyStore
            .entities(MethodFieldReadAccessInformation.key)
            .filter(ai => projectClassFiles.contains(ai.e.asInstanceOf[Method].classFile))
            .map(_.asFinal.p.numIncompleteAccessSites)
            .sum

        s"""
           |
           | Not Accessed Fields: ${fields.size - purelyReadFields.size - purelyWrittenFields.size - readAndWrittenFields.size}
           | Purely Read Fields : ${purelyReadFields.size}
           | Purely Written Fields: ${purelyWrittenFields.size}
           | Read And Written Fields: ${readAndWrittenFields.size}
           |
           | Access Sites with missing information: $totalIncompleteAccessSiteCount
           |
           | total Fields: ${fields.size}
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
