/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.tac.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.tac.Assignment
import org.opalj.tac.DVar
import org.opalj.tac.MonitorEnter
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Finds object references in monitorenter instructions that do not escape their thread.
 *
 * @author Florian Kübler
 */
object UnnecessarySynchronizationAnalysis extends ProjectAnalysisApplication {

    override def title: String = "Finds unnecessary usages of synchronization"

    override def description: String = {
        "Finds unnecessary usages of synchronization"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext: LogContext = project.logContext

        val propertyStore = project.get(PropertyStoreKey)
        val manager = project.get(FPCFAnalysesManagerKey)
        time {
            manager.runAll(
                RTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                TriggeredConfiguredNativeMethodsAnalysis,
                TriggeredSystemPropertiesAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer
            )
        } { t ⇒ info("progress", s"computing call graph and tac took ${t.toSeconds}") }
        time {
            manager.runAll(
                EagerInterProceduralEscapeAnalysis
            )
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }

        val allocationSites = project.get(DefinitionSitesKey).getAllocationSites
        val objects = time {
            for {
                as ← allocationSites
                method = as.method
                FinalP(escape) = propertyStore(as, EscapeProperty.key)
                if EscapeViaNormalAndAbnormalReturn lessOrEqualRestrictive escape
                FinalP(tacai) = propertyStore(method, TACAI.key)
                code = tacai.tac.get.stmts
                defSite = code indexWhere (stmt ⇒ stmt.pc == as.pc)
                if defSite != -1
                stmt = code(defSite)
                if stmt.astID == Assignment.ASTID
                Assignment(_, DVar(_, uses), New(_, _) | NewArray(_, _, _)) = code(defSite)
                if uses exists { use ⇒
                    code(use) match {
                        case MonitorEnter(_, v) if v.asVar.definedBy.contains(defSite) ⇒ true
                        case _ ⇒ false
                    }
                }
            } yield as
        } { t ⇒ info("progress", s"unnecessary synchronization analysis took ${t.toSeconds}") }

        val message =
            s"""|Objects that were unnecessarily synchronized:
                |${objects.mkString("\n|")}
             """

        BasicReport(message.stripMargin('|'))
    }

}
