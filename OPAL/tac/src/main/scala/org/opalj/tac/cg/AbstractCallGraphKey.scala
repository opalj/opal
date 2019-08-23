/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.reflect.runtime.universe.runtimeMirror

import scala.collection.JavaConverters._

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider

/**
 * An abstract [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]].
 * Uses the call graph analyses modules specified in the config file under the key
 * "org.opalj.tac.cg.CallGraphKey.modules".
 *
 * @author Florian Kuebler
 */
trait AbstractCallGraphKey extends ProjectInformationKey[CallGraph, Nothing] {

    /**
     * Lists the call graph specific schedulers that must be run to compute the respective call
     * graph.
     */
    protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]]

    override def requirements: ProjectInformationKeys = {
        Seq(
            DeclaredMethodsKey,
            InitialEntryPointsKey,
            PropertyStoreKey,
            FPCFAnalysesManagerKey
        )
    }

    override def compute(project: SomeProject): CallGraph = {
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)
        implicit val logContext = project.logContext

        val manager = project.get(FPCFAnalysesManagerKey)

        val config = project.config

        // TODO use FPCFAnaylsesRegistry here
        val registeredAnalyses = config.getStringList(
            "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_))

        // TODO make TACAI analysis configurable
        var analyses: List[ComputationSpecification[FPCFAnalysis]] =
            List(
                LazyTACAIProvider
            )

        analyses ++= callGraphSchedulers(project)
        analyses ++= registeredAnalyses

        manager.runAll(analyses)

        new CallGraph()
    }

    private[this] def resolveAnalysisRunner(
        className: String
    )(implicit logContext: LogContext): Option[FPCFAnalysisScheduler] = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(className)
            import mirror.reflectModule
            Some(reflectModule(module).instance.asInstanceOf[FPCFAnalysisScheduler])
        } catch {
            case sre: ScalaReflectionException ⇒
                error("call graph", s"cannot find analysis scheduler $className", sre)
                None
            case cce: ClassCastException ⇒
                error("call graph", "analysis scheduler class is invalid", cce)
                None
        }
    }
}
