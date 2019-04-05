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
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on the class hierarchy (CHA).
 * Uses the call graph analyses modules specified in the config file under the key
 * "org.opalj.tac.cg.CallGraphKey.modules".
 *
 *
 * @author Florian Kuebler
 *
 */
object CHACallGraphKey extends ProjectInformationKey[CallGraph, Nothing] {

    override protected def requirements: ProjectInformationKeys = {
        Seq(
            DeclaredMethodsKey,
            InitialEntryPointsKey,
            InitialInstantiatedTypesKey,
            PropertyStoreKey,
            FPCFAnalysesManagerKey
        )
    }

    override protected def compute(project: SomeProject): CallGraph = {
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)
        implicit val logContext = project.logContext

        val manager = project.get(FPCFAnalysesManagerKey)

        val config = project.config

        // todo use registry here
        val registeredModules = config.getStringList(
            "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_))

        var analyses: List[ComputationSpecification[FPCFAnalysis]] =
            List(CHACallGraphAnalysisScheduler, LazyTACAIProvider)

        analyses ++= registeredModules

        manager.runAll(analyses)

        new CallGraph()
    }

    private[this] def resolveAnalysisRunner(fqn: String)(implicit logContext: LogContext): Option[FPCFAnalysisScheduler] = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(fqn)
            import mirror.reflectModule
            Some(reflectModule(module).instance.asInstanceOf[FPCFAnalysisScheduler])
        } catch {
            case sre: ScalaReflectionException ⇒
                error("CHA call graph", "cannot find analysis scheduler", sre)
                None
            case cce: ClassCastException ⇒
                error("CHA call graph", "analysis scheduler class is invalid", cce)
                None
        }
    }
}
