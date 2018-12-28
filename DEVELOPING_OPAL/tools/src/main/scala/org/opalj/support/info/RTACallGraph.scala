/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.TriggeredSystemPropertiesAnalysis
import org.opalj.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis
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
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Computes a RTA based call graph and reports its size.
 *
 * @author Florian Kuebler
 */
object RTACallGraph extends DefaultOneStepAnalysis {

    override def title: String = "Field Locality"

    override def description: String = {
        "Provides lifetime information about the values stored in instance fields."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val (ps, _ /*executed analyses*/ ) = project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0BaseAIAnalysis,
            TACAITransformer,
            /* Call Graph Analyses */
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
            EagerLibraryEntryPointsAnalysis,
            LazyCalleesAnalysis(
                Set(StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees)
            )
        )

        val allMethods = project.get(DeclaredMethodsKey).declaredMethods

        val callersProperties = ps(allMethods.toTraversable, CallersProperty.key)
        assert(callersProperties.forall(_.isFinal))

        val reachableMethods = callersProperties.filterNot(_.ub eq NoCallers).map(_.ub)

        val numEdges = reachableMethods.foldLeft(0) { (accEdges, callersProperty) ⇒
            callersProperty.callers.size + accEdges
        }

        val message =
            s"""|# of methods: ${allMethods.size}
                |# of reachable methods: ${reachableMethods.size}
                |# of call edges: $numEdges
                |"""

        BasicReport(message.stripMargin('|'))
    }
}
