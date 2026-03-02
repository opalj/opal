/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.log.OPALLogger.info
import org.opalj.tac.Assignment
import org.opalj.tac.DVar
import org.opalj.tac.MonitorEnter
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.cg.CGBasedCommandLineConfig
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.util.PerformanceEvaluation.time

/**
 * Finds object references in monitorenter instructions that do not escape their thread.
 *
 * @author Florian Kuebler
 */
object UnnecessarySynchronizationAnalysis extends ProjectsAnalysisApplication {

    protected class UnnecessarySynchronizationConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with CGBasedCommandLineConfig {
        val description = "Finds synchronized(o){ ... } statements where the object o does not escape the thread"
    }

    protected type ConfigType = UnnecessarySynchronizationConfig

    protected def createConfig(args: Array[String]): UnnecessarySynchronizationConfig =
        new UnnecessarySynchronizationConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: UnnecessarySynchronizationConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)
        val (propertyStore, _) = analysisConfig.setupPropertyStore(project)
        analysisConfig.setupCallGraph(project)

        time {
            project.get(FPCFAnalysesManagerKey).runAll(
                EagerInterProceduralEscapeAnalysis
            )
        } { t => info("progress", s"escape analysis took ${t.toSeconds}")(using project.logContext) }

        val allocationSites = project.get(DefinitionSitesKey).getAllocationSites
        val objects = time {
            for {
                as <- allocationSites
                method = as.method
                FinalP(escape) = propertyStore(as, EscapeProperty.key): @unchecked
                if EscapeViaNormalAndAbnormalReturn.lessOrEqualRestrictive(escape)
                FinalP(tacai) = propertyStore(method, TACAI.key): @unchecked
                code = tacai.tac.get.stmts
                defSite = code indexWhere (stmt => stmt.pc == as.pc)
                if defSite != -1
                stmt = code(defSite)
                if stmt.astID == Assignment.ASTID
                Assignment(_, DVar(_, uses), New(_, _) | NewArray(_, _, _)) = code(defSite): @unchecked
                if uses exists { use =>
                    code(use) match {
                        case MonitorEnter(_, v) if v.asVar.definedBy.contains(defSite) => true
                        case _                                                         => false
                    }
                }
            } yield as
        } { t => info("progress", s"unnecessary synchronization analysis took ${t.toSeconds}")(using project.logContext) }

        val message =
            s"""|Objects that were unnecessarily synchronized:
                |${objects.mkString("\n|")}
             """

        (project, BasicReport(message.stripMargin('|')))
    }

}
