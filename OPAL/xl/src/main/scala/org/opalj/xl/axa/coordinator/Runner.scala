/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package axa
package coordinator

import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.RTATypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.xl.analyses.javaAnalysis.adaptor.EagerJavaJavaScriptAdaptor
import org.opalj.xl.analyses.javaAnalysis.analysis.JavaTainted
import org.opalj.xl.analyses.javascript.analyses.LazyJavaScriptAnalysis
import org.opalj.xl.axa.proxy.TriggeredFrameworkProxyScheduler

import java.net.URL
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.runtime.universe.runtimeMirror

object AnalysesRunner extends AnalysisApplication with OneStepAnalysis[URL, ReportableAnalysisResult] { //},[URL] ReportableAnalysisResult] {

    implicit val logContext: LogContext = GlobalLogContext

    final override val analysis = this

    override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): BasicReport = {

    var analyses: List[FPCFAnalysisScheduler] =
      List(
        LazyTACAIProvider
      )

    def resolveAnalysisRunner(
        className: String
    )(implicit logContext: LogContext): Option[FPCFAnalysisScheduler] = {
      val mirror = runtimeMirror(getClass.getClassLoader)
      try {
        val module = mirror.staticModule(className)
        import mirror.reflectModule
        Some(reflectModule(module).instance.asInstanceOf[FPCFAnalysisScheduler])
      } catch {
        case sre: ScalaReflectionException =>
          error("call graph", s"cannot find analysis scheduler $className", sre)
          None
        case cce: ClassCastException =>
          error("call graph", "analysis scheduler class is invalid", cce)
          None
      }
    }

      def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnaylsesRegistry here
        config.getStringList(
          "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_)).toSeq
      }



    project.updateProjectInformationKeyInitializationData(TypeIteratorKey) { case _ =>
      () => new RTATypeIterator(project)
    }

    analyses ::= CallGraphAnalysisScheduler
    analyses ++= RTACallGraphKey.callGraphSchedulers(project)
    analyses ++= registeredAnalyses(project)
      analyses ++= Iterable(AllocationSiteBasedPointsToAnalysisScheduler,
        EagerJavaJavaScriptAdaptor,
      LazyJavaScriptAnalysis, TriggeredFrameworkProxyScheduler)

          val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(
       analyses
          )



val javaTaintedVariables = propertyStore.finalEntities(JavaTainted)

BasicReport(
 " \n"+
     s"""
    | Results of Java Taint Analysis:
    |Tainted variables: ${javaTaintedVariables.mkString("\n")}
    |
    |
    |""".stripMargin //
)
}
}
