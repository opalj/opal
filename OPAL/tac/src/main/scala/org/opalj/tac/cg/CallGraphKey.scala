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
import org.opalj.br.analyses.{DeclaredMethodsKey, JavaProjectInformationKey, JavaProjectInformationKeys, SomeProject}
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.CallBySignatureKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.JavaFPCFAnalysisScheduler
import org.opalj.fpcf.scheduling.FPCFAnalysesManagerKey
import org.opalj.si.PropertyStoreKey
import org.opalj.tac.fpcf.analyses.LazyTACAIProvider
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

/**
 * An abstract [[JavaProjectInformationKey]] to compute a [[CallGraph]].
 * Uses the call graph analyses modules specified in the config file under the key
 * "org.opalj.tac.cg.CallGraphKey.modules".
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
trait CallGraphKey extends JavaProjectInformationKey[CallGraph, Nothing] {

    private[this] val CallBySignatureConfigKey = "org.opalj.br.analyses.cg.callBySignatureResolution"

    private[this] var typeIterator: TypeIterator = null

    /**
     * Lists the call graph specific schedulers that must be run to compute the respective call
     * graph.
     */
    protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[JavaFPCFAnalysisScheduler]

    override def requirements(project: SomeProject): JavaProjectInformationKeys = {
        project.updateProjectInformationKeyInitializationData(TypeIteratorKey) {
            case Some(typeIterator: TypeIterator) if typeIterator ne this.typeIterator =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    s"must not configure multiple type iterators"
                )
                throw new IllegalArgumentException()
            case Some(_) => () => this.typeIterator
            case None => () => {
                this.typeIterator = getTypeIterator(project)
                this.typeIterator
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

    protected[this] def registeredAnalyses(project: SomeProject): scala.collection.Seq[JavaFPCFAnalysisScheduler] = {
        implicit val logContext: LogContext = project.logContext
        val config = project.config

        // TODO use FPCFAnaylsesRegistry here
        config.getStringList(
            "org.opalj.tac.cg.CallGraphKey.modules"
        ).asScala.flatMap(resolveAnalysisRunner(_))
    }

    override def compute(project: SomeProject): CallGraph = {
        implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
        implicit val ps: PropertyStore = project.get(PropertyStoreKey)

        val manager = project.get(FPCFAnalysesManagerKey)

        // TODO make TACAI analysis configurable
        var analyses: List[JavaFPCFAnalysisScheduler] =
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
    )(implicit logContext: LogContext): Option[JavaFPCFAnalysisScheduler] = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(className)
            import mirror.reflectModule
            Some(reflectModule(module).instance.asInstanceOf[JavaFPCFAnalysisScheduler])
        } catch {
            case sre: ScalaReflectionException =>
                error("call graph", s"cannot find analysis scheduler $className", sre)
                None
            case cce: ClassCastException =>
                error("call graph", "analysis scheduler class is invalid", cce)
                None
        }
    }

    private[this] def requiresCallBySignatureKey(p: SomeProject): JavaProjectInformationKeys = {
        val config = p.config
        if (config.hasPath(CallBySignatureConfigKey)
            && config.getBoolean(CallBySignatureConfigKey)) {
            return Seq(CallBySignatureKey);
        }
        Seq.empty
    }

    def getTypeIterator(project: SomeProject): TypeIterator
}

object CallGraphKey extends JavaProjectInformationKey[CallGraph, CallGraph] {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = Seq(TypeIteratorKey)

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