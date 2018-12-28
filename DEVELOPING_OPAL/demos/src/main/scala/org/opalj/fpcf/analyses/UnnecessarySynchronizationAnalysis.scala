/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredConfiguredNativeMethodsAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredInstantiatedTypesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.TriggeredSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.reflection.TriggeredReflectionRelatedCallsAnalysis
import org.opalj.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.tac.Assignment
import org.opalj.tac.DVar
import org.opalj.tac.MonitorEnter
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.fpcf.analyses.TACAITransformer
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.util.PerformanceEvaluation.time

/**
 * Finds object references in monitorenter instructions that do not escape their thread.
 *
 * @author Florian Kübler
 */
object UnnecessarySynchronizationAnalysis extends DefaultOneStepAnalysis {

    override def title: String = "Finds unnecessary usage of synchronization"

    override def description: String = {
        "Finds unnecessary usage of synchronization"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        implicit val logContext: LogContext = project.logContext

        val propertyStore = time {
            project.get(PropertyStoreKey)
        } { t ⇒ info("progress", s"initialization of property store took ${t.toSeconds}") }

        val manager = project.get(FPCFAnalysesManagerKey)
        time {
            manager.runAll(
                TriggeredRTACallGraphAnalysisScheduler,
                TriggeredStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                TriggeredFinalizerAnalysisScheduler,
                TriggeredThreadRelatedCallsAnalysis,
                TriggeredSerializationRelatedCallsAnalysis,
                TriggeredReflectionRelatedCallsAnalysis,
                TriggeredInstantiatedTypesAnalysis,
                TriggeredConfiguredNativeMethodsAnalysis,
                TriggeredSystemPropertiesAnalysis,
                LazyCalleesAnalysis(
                    Set(StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees)
                ),
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
