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
import org.opalj.fpcf.analyses.cg.EagerFinalizerAnalysisScheduler
import org.opalj.fpcf.analyses.cg.EagerLoadedClassesAnalysis
import org.opalj.fpcf.analyses.cg.EagerRTACallGraphAnalysisScheduler
import org.opalj.fpcf.analyses.cg.LazyCalleesAnalysis
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CalleesLikePropertyMetaInformation
import org.opalj.fpcf.properties.StandardInvokeCallees
//import org.opalj.fpcf.par.PKEParallelTasksPropertyStore
//import org.opalj.fpcf.par.RecordAllPropertyStoreTracer
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.LoadedClasses
import org.opalj.fpcf.properties.VMReachableFinalizers
import org.opalj.log.OPALLogger.info
import org.opalj.tac.SimpleTACAIKey
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
            val tac = project.get(SimpleTACAIKey)
            project.parForeachMethodWithBody() { m ⇒ tac(m.method) }
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        // Get the TAC code for all methods to make it possible to measure the time for
        // the analysis itself.
        time {
            val manager = project.get(FPCFAnalysesManagerKey)
            manager.runAll(
                EagerRTACallGraphAnalysisScheduler,
                EagerLoadedClassesAnalysis,
                EagerFinalizerAnalysisScheduler,
                new LazyCalleesAnalysis(Set(StandardInvokeCallees.asInstanceOf[CalleesLikePropertyMetaInformation]))
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
