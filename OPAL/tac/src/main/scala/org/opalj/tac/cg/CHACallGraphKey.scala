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
import org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on the class hierarchy (CHA).
 * Uses the call graph analyses modules specified in the config file under the key
 * "org.opalj.tac.cg.CallGraphKey.modules".
 *
 * @param isLibrary
 *      should the [[org.opalj.tac.fpcf.analyses.cg.EagerLibraryEntryPointsAnalysis]] be scheduled?
 *
 *      Note, that initial instantiated types ([[InitialInstantiatedTypesKey]]) and entry points
 *      ([[InitialEntryPointsKey]]) can be configured before hand.
 *      Furthermore, you can configure the analysis mode (Library or Application) in the configuration
 *      of these keys.
 *
 *
 *
 * @author Florian Kuebler
 *
 */
case class CHACallGraphKey(
        isLibrary: Boolean
) extends ProjectInformationKey[CallGraph, Nothing] {

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

        if (isLibrary)
            analyses ::= EagerLibraryEntryPointsAnalysis

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
                error("RTA call graph", "cannot find analysis scheduler", sre)
                None
            case cce: ClassCastException ⇒
                error("RTA call graph", "analysis scheduler class is invalid", cce)
                None
        }
    }
}
