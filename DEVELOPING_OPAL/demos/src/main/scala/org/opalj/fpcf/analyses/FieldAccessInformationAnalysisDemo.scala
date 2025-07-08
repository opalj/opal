/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.io.File

import org.rogach.scallop.ScallopConf

import org.opalj.br.DeclaredField
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.NoFieldWriteAccessInformation
import org.opalj.bytecode.JDKPackages
import org.opalj.tac.cg.CallGraphArg
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.reflection.ReflectionRelatedFieldAccessesAnalysisScheduler
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

/**
 * Runs analyses for field accesses throughout a project and automatically excludes any JDK files included in the
 * project files from the summary at the end.
 *
 * @author Maximilian Rüsch
 */
object FieldAccessInformationAnalysisDemo extends ProjectsAnalysisApplication {

    protected class FieldAccessConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {

        val description = "Computes information about field accesses (reads and writes)"

        init()
    }

    protected type ConfigType = FieldAccessConfig

    protected def createConfig(args: Array[String]): FieldAccessConfig = new FieldAccessConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: FieldAccessConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (propertyStore, _) = analysisConfig.setupPropertyStore(project)

        var analysisTime: Seconds = Seconds.None

        val callGraphKey = analysisConfig(CallGraphArg)
        val typeIterator = callGraphKey.getTypeIterator(project)
        project.updateProjectInformationKeyInitializationData(ContextProviderKey) { _ => typeIterator }

        time {
            project.get(FPCFAnalysesManagerKey)
                .runAll(
                    callGraphKey.allCallGraphAnalyses(project)
                        ++ Set(
                            EagerFieldAccessInformationAnalysis,
                            ReflectionRelatedFieldAccessesAnalysisScheduler
                        )
                )
            propertyStore.waitOnPhaseCompletion()
        } { t => analysisTime = t.toSeconds }

        val projectTypes = project.allProjectClassFiles.iterator.collect {
            case cf if !JDKPackages.exists(cf.thisType.packageName.startsWith) => cf.thisType
        }.toSet

        val readFields = propertyStore
            .entities(FieldReadAccessInformation.key)
            .collect {
                case FinalEP(f: DeclaredField, p)
                    if projectTypes.contains(f.declaringClassType) && p != NoFieldReadAccessInformation => f
            }
            .toSet
        val writtenFields = propertyStore
            .entities(FieldWriteAccessInformation.key)
            .collect {
                case FinalEP(f: DeclaredField, p)
                    if projectTypes.contains(f.declaringClassType) && p != NoFieldWriteAccessInformation => f
            }
            .toSet

        val fields = project.get(DeclaredFieldsKey).declaredFields.toSet
        val readAndWrittenFields = readFields intersect writtenFields
        val purelyReadFields = readFields diff readAndWrittenFields
        val purelyWrittenFields = writtenFields diff readAndWrittenFields
        val notAccessedFields = fields diff readFields diff writtenFields

        val totalIncompleteAccessSiteCount = propertyStore
            .entities(MethodFieldReadAccessInformation.key)
            .filter(ai => projectTypes.contains(ai.e.asInstanceOf[Method].classFile.thisType))
            .map(_.asFinal.p.numIncompleteAccessSites)
            .sum

        def getFieldsList(fields: Set[DeclaredField]): String = {
            if (fields.size > 50) "\n|     Too many fields to display!"
            else fields.iterator.map(f => s"- ${f.name}").mkString("\n|     ", "\n|     ", "")
        }

        val result =
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

        (project, BasicReport(result))
    }
}
