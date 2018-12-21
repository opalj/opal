/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.fpcf.analyses.cg.EagerConfiguredNativeMethodsAnalysis
import org.opalj.fpcf.analyses.cg.EagerFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerInstantiatedTypesAnalysis
import org.opalj.fpcf.analyses.cg.EagerStaticInitializerAnalysis
import org.opalj.fpcf.analyses.cg.EagerRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerSerializationRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.EagerThreadRelatedCallsAnalysis
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.analyses.cg.TriggeredLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.reflection.EagerReflectionRelatedCallsAnalysis
import org.opalj.fpcf.cg.properties.LoadedClasses
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.VMReachableFinalizers
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.cg.properties.StandardInvokeCallees
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.ReflectionRelatedCallees
import org.opalj.tac.fpcf.analyses.LazyL0TACAIAnalysis
//import org.opalj.fpcf.par.PKEParallelTasksPropertyStore
//import org.opalj.fpcf.par.RecordAllPropertyStoreTracer
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time

object RTADemo extends DefaultOneStepAnalysis {
    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): ReportableAnalysisResult = {

        /*project.getOrCreateProjectInformationKeyInitializationData(
            PropertyStoreKey,
            (context: List[PropertyStoreContext[AnyRef]]) ⇒ {
                val ps = PKEParallelTasksPropertyStore.create(
                    new RecordAllPropertyStoreTracer,
                    context.iterator.map(_.asTuple).toMap
                )(project.logContext)
                PropertyStore.updateDebug(true)
                ps
            }
        )*/

        val ps = project.get(PropertyStoreKey)
        PropertyStore.updateDebug(false)
        //PropertyStore.updateTraceCycleResolutions(true)
        //PropertyStore.updateTraceFallbacks(true)

        implicit val logContext = project.logContext

        // Get the TAC code for all methods to make it possible to measure the time for
        // the analysis itself.
        time {
            val manager = project.get(FPCFAnalysesManagerKey)
            manager.runAll(
                EagerRTACallGraphAnalysisScheduler,
                EagerStaticInitializerAnalysis,
                TriggeredLoadedClassesAnalysis,
                EagerFinalizerAnalysisScheduler,
                EagerThreadRelatedCallsAnalysis,
                EagerSerializationRelatedCallsAnalysis,
                EagerReflectionRelatedCallsAnalysis,
                SystemPropertiesAnalysis,
                EagerConfiguredNativeMethodsAnalysis,
                EagerInstantiatedTypesAnalysis,
                LazyL0TACAIAnalysis,
                new LazyCalleesAnalysis(
                    Set(StandardInvokeCallees, SerializationRelatedCallees, ReflectionRelatedCallees)
                )
            )
        } { t ⇒ info("progress", s"constructing the call graph took ${t.toSeconds}") }

        val declaredMethods = project.get(DeclaredMethodsKey)
        for (dm ← declaredMethods.declaredMethods) {
            ps(dm, Callees.key)
        }

        ps.waitOnPhaseCompletion()

        println(ps(project, InstantiatedTypes.key).ub)
        println(ps(project, LoadedClasses.key).ub)
        println(ps(project, VMReachableFinalizers.key).ub)
        for (m ← project.allMethods) {
            val dm = declaredMethods(m)
            ps(dm, CallersProperty.key)
            /*if (callers.isFinal)
                println(callers)*/
            ps(dm, Callees.key)
            /*if (callees.isFinal)
                println(callees)*/
        }
        // for (m <- project.allMethods) {
        //    println(ps(m, Callees.key))
        //}

        println(ps.statistics.mkString("\n"))

        BasicReport("")
    }
}
