/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.reflect.runtime.universe.runtimeMirror

import scala.jdk.CollectionConverters._

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.cg.CallBySignatureKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TypeProvider

/**
 * An abstract [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]].
 * Uses the call graph analyses modules specified in the config file under the key
 * "org.opalj.tac.cg.CallGraphKey.modules".
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
trait CallGraphKey extends ProjectInformationKey[CallGraph, Nothing] {

    private[this] val CallBySignatureConfigKey = "org.opalj.br.analyses.cg.callBySignatureResolution"

    private[this] var typeProvider: TypeProvider = null

    /**
     * Lists the call graph specific schedulers that must be run to compute the respective call
     * graph.
     */
    protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler]

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        project.updateProjectInformationKeyInitializationData(TypeProviderKey) {
            case Some(typeProvider: TypeProvider) if typeProvider ne this.typeProvider =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    s"must not configure multiple type providers"
                )
                throw new IllegalArgumentException()
            case Some(_) => () => this.typeProvider
            case None => () => {
                this.typeProvider = getTypeProvider(project)
                this.typeProvider
            }
        }

        Seq(
            DeclaredMethodsKey,
            InitialEntryPointsKey,
            IsOverridableMethodKey,
            PropertyStoreKey,
            FPCFAnalysesManagerKey
        ) ++
            requiresCallBySignatureKey(project) ++
            CallGraphAnalysisScheduler.requiredProjectInformation ++
            callGraphSchedulers(project).flatMap(_.requiredProjectInformation) ++
            registeredAnalyses(project).flatMap(_.requiredProjectInformation)
    }

    protected[this] def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnaylsesRegistry here
        config.getStringList(
            "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_))
    }

    override def compute(project: SomeProject): CallGraph = {
        implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val manager = project.get(FPCFAnalysesManagerKey)

        // TODO make TACAI analysis configurable
        var analyses: List[FPCFAnalysisScheduler] =
            List(
                LazyTACAIProvider
            )

        analyses ::= CallGraphAnalysisScheduler
        analyses ++= callGraphSchedulers(project)
        analyses ++= registeredAnalyses(project)

        manager.runAll(analyses)

        val cg = new CallGraph()

        project.updateProjectInformationKeyInitializationData(CallGraphKey) {
            case Some(_) =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    s"must not compute multiple call graphs"
                )
                throw new IllegalArgumentException()
            case None => cg
        }

        cg
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
            case sre: ScalaReflectionException =>
                error("call graph", s"cannot find analysis scheduler $className", sre)
                None
            case cce: ClassCastException =>
                error("call graph", "analysis scheduler class is invalid", cce)
                None
        }
    }

    private[this] def requiresCallBySignatureKey(p: SomeProject): ProjectInformationKeys = {
        val config = p.config
        if (config.hasPath(CallBySignatureConfigKey)
            && config.getBoolean(CallBySignatureConfigKey)) {
            return Seq(CallBySignatureKey);
        }
        Seq.empty
    }

    def getTypeProvider(project: SomeProject): TypeProvider
}

object CallGraphKey extends ProjectInformationKey[CallGraph, CallGraph] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(TypeProviderKey)

    override def compute(project: SomeProject): CallGraph = {

        project.getProjectInformationKeyInitializationData(this) match {
            case Some(cg) =>
                cg
            case None =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    s"must compute specific call graph first"
                )
                throw new IllegalArgumentException()
        }
    }
}