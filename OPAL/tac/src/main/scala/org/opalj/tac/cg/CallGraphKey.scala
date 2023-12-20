/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import scala.reflect.runtime.universe.runtimeMirror

import scala.jdk.CollectionConverters._

import org.opalj.ai.domain.RecordCFG
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.CallBySignatureKey
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.fpcf.PropertyStore
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

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

    private[this] var typeIterator: TypeIterator = null

    /**
     * Lists the call graph specific schedulers that must be run to compute the respective call
     * graph.
     */
    protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler]

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        val requiredDomains: Set[Class[_ <: AnyRef]] = Set(classOf[RecordCFG], classOf[RecordDefUse])
        project.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => requiredDomains
            case Some(requirements) => requirements ++ requiredDomains
        }

        project.updateProjectInformationKeyInitializationData(ContextProviderKey) {
            case Some(typeIterator: TypeIterator) =>
                if (typeIterator ne this.typeIterator) {
                    implicit val logContext: LogContext = project.logContext
                    OPALLogger.error(
                        "analysis configuration",
                        s"must not configure multiple type iterators"
                    )
                    throw new IllegalArgumentException()
                }
                this.typeIterator
            case Some(_: ContextProvider) =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    "a context provider has already been established"
                )
                throw new IllegalStateException()
            case None =>
                this.typeIterator = getTypeIterator(project)
                this.typeIterator
        }

        Seq(
            DeclaredMethodsKey,
            InitialEntryPointsKey,
            IsOverridableMethodKey,
            PropertyStoreKey,
            FPCFAnalysesManagerKey
        ) ++
            requiresCallBySignatureKey(project) ++
            allCallGraphAnalyses(project).flatMap(_.requiredProjectInformation)
    }

    protected[this] def registeredAnalyses(project: SomeProject): scala.collection.Seq[FPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnaylsesRegistry here
        config.getStringList(
            "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_))
    }

    def allCallGraphAnalyses(project: SomeProject): Iterable[FPCFAnalysisScheduler] = {
        // TODO make TACAI analysis configurable
        var analyses: List[FPCFAnalysisScheduler] =
            List(
                LazyTACAIProvider
            )

        analyses ::= CallGraphAnalysisScheduler
        analyses ++= callGraphSchedulers(project)
        analyses ++= registeredAnalyses(project)

        analyses
    }

    override def compute(project: SomeProject): CallGraph = {
        if (CallGraphKey.cg.isDefined && project.availableProjectInformation.contains(CallGraphKey.cg.get)) {
            implicit val logContext: LogContext = project.logContext
            OPALLogger.error(
                "analysis configuration",
                s"must not compute multiple call graphs"
            )
            throw new IllegalArgumentException()
        }

        implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        runAnalyses(project, ps)

        val cg = new CallGraph()

        CallGraphKey.cg = Some(cg)

        cg
    }

    protected[this] def runAnalyses(project: SomeProject, ps: PropertyStore): Unit = {
        val manager = project.get(FPCFAnalysesManagerKey)
        manager.runAll(allCallGraphAnalyses(project))
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

    def getTypeIterator(project: SomeProject): TypeIterator
}

object CallGraphKey extends ProjectInformationKey[CallGraph, CallGraph] {

    private var cg: Option[CallGraph] = None

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(TypeIteratorKey)

    override def compute(project: SomeProject): CallGraph = {
        if (cg.isDefined && project.availableProjectInformation.contains(cg.get)) {
            cg.get
        } else {
            implicit val logContext: LogContext = project.logContext
            OPALLogger.error(
                "analysis configuration",
                s"must compute specific call graph first"
            )
            cg = None
            throw new IllegalArgumentException()
        }
    }
}
