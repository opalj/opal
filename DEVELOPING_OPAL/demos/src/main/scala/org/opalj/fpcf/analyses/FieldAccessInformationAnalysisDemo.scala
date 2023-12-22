/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.DefinedField
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldWriteAccessInformation
import org.opalj.tac.cg.XTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

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

    override def title: String = "Determines field accesses"

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

        val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        var analysisTime: Seconds = Seconds.None
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        val typeIterator = XTACallGraphKey.getTypeIterator(project)
        project.updateProjectInformationKeyInitializationData(ContextProviderKey) { _ => typeIterator }

        time {
            analysesManager
                .runAll(
                    XTACallGraphKey.allCallGraphAnalyses(project)
                        ++ Set(
                            EagerFieldAccessInformationAnalysis,
                            ReflectionRelatedFieldAccessesAnalysisScheduler
                        )
                )
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
            .filter(ep => fields.contains(ep.e.asInstanceOf[DefinedField].definedField)
                && ep.asFinal.p != NoFieldReadAccessInformation)
            .map(_.e.asInstanceOf[DefinedField].definedField)
            .toSet
        val writtenFields = propertyStore
            .entities(FieldWriteAccessInformation.key)
            .filter(ep => fields.contains(ep.e.asInstanceOf[DefinedField].definedField)
                && ep.asFinal.p != NoFieldWriteAccessInformation)
            .map(_.e.asInstanceOf[DefinedField].definedField)
            .toSet

        val readAndWrittenFields = readFields intersect writtenFields
        val purelyReadFields = readFields diff readAndWrittenFields
        val purelyWrittenFields = writtenFields diff readAndWrittenFields
        val notAccessedFields = fields diff readFields diff writtenFields

        val totalIncompleteAccessSiteCount = propertyStore
            .entities(MethodFieldReadAccessInformation.key)
            .filter(ai => projectClassFiles.contains(ai.e.asInstanceOf[Method].classFile))
            .map(_.asFinal.p.numIncompleteAccessSites)
            .sum

        def getFieldsList(fields: Set[Field]): String = {
            if (fields.size > 50) "\n|     Too many fields to display!"
            else fields.iterator.map(f => s"- ${f.name}").mkString("\n|     ", "\n|     ", "")
        }

        s"""
           |
           | Not Accessed Fields: ${notAccessedFields.size} ${getFieldsList(notAccessedFields)}
           | Purely Read Fields : ${purelyReadFields.size} ${getFieldsList(purelyReadFields)}
           | Purely Written Fields: ${purelyWrittenFields.size} ${getFieldsList(purelyWrittenFields)}
           | Read And Written Fields: ${readAndWrittenFields.size} ${getFieldsList(readAndWrittenFields)}
           |
           | Access Sites with missing information: $totalIncompleteAccessSiteCount
           |
           | total Fields: ${fields.size}
           | took : $analysisTime seconds
           |""".stripMargin
    }
}
