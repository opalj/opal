/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import scala.reflect.runtime.universe.runtimeMirror

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValueFactory

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.log.OPALLogger.info

/**
 * Registry for all factories for analyses that are implemented using the fixpoint computations
 * framework ('''FPCF''').
 *
 * The registry primarily serves as a central container that can be queried by subsequent tools.
 *
 * The analyses that are part of OPAL are already registered.
 *
 * @note Analysis schedules can be computed using the `PropertiesComputationsScheduler`.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
object FPCFAnalysesRegistry {

    private[this] implicit def logContext: LogContext = GlobalLogContext

    private[this] var idToEagerScheduler: Map[String, FPCFEagerAnalysisScheduler] = Map.empty
    private[this] var idToLazyScheduler: Map[String, FPCFLazyAnalysisScheduler] = Map.empty
    private[this] var idToTriggeredScheduler: Map[String, FPCFTriggeredAnalysisScheduler] = Map.empty
    private[this] var idToDescription: Map[String, String] = Map.empty
    private[this] var propertyToDefaultScheduler: Map[PropertyBounds, FPCFAnalysisScheduler] = Map.empty

    /**
     * Registers the factory of a fixpoint analysis that can be
     * used to compute a specific (set of) property(ies).
     *
     * @param analysisDescription A short description of the analysis and the properties that the
     *                            analysis computes; in particular w.r.t. a specific set of entities.
     * @param analysisFactory     The factory.
     * @param lazyFactory         Register the analysis factory as lazy analysis factory.
     */
    def register(
        analysisID:          String,
        analysisDescription: String,
        analysisFactory:     String,
        factoryType:         String,
        default:             Boolean
    ): Unit = this.synchronized {
        resolveAnalysisRunner(analysisFactory) match {
            case Some(analysisRunner) =>
                factoryType match {
                    case "lazy" =>
                        idToLazyScheduler += ((analysisID, analysisRunner.asInstanceOf[FPCFLazyAnalysisScheduler]))
                    case "triggered" =>
                        idToTriggeredScheduler += ((
                            analysisID,
                            analysisRunner.asInstanceOf[FPCFTriggeredAnalysisScheduler]
                        ))
                    case "eager" =>
                        idToEagerScheduler += ((analysisID, analysisRunner.asInstanceOf[FPCFEagerAnalysisScheduler]))
                    case _ =>
                        OPALLogger.error("project configuration", s"Unknown analysis factory type ${factoryType}")
                }

                if (default)
                    analysisRunner.derives.foreach { p =>
                        if (propertyToDefaultScheduler.contains(p)) {
                            val message = s"cannot register ${analysisRunner.name} " +
                                s"as default analysis for ${PropertyKey.name(p.pk)}, " +
                                s"${propertyToDefaultScheduler(p).name} was already registered"
                            error("OPAL Setup", message)
                        } else {
                            propertyToDefaultScheduler += ((p, analysisRunner))
                        }
                    }

                idToDescription += ((analysisID, analysisDescription))
                val message =
                    s"registered $factoryType analysis: $analysisID ($analysisDescription)"
                info("OPAL Setup", message)

            case None =>
                error("OPAL Setup", s"unknown analysis implementation: $analysisFactory")
        }
    }

    private[this] def resolveAnalysisRunner(fqn: String): Option[FPCFAnalysisScheduler] = {
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(fqn)
            import mirror.reflectModule
            Some(reflectModule(module).instance.asInstanceOf[FPCFAnalysisScheduler])
        } catch {
            case sre: ScalaReflectionException =>
                error("FPCF registry", "cannot find analysis scheduler", sre)
                None
            case cce: ClassCastException =>
                error("FPCF registry", "analysis scheduler class is invalid", cce)
                None
        }
    }

    def registerFromConfig(): Unit = {
        val config = ConfigFactory.load()
        try {
            val registeredAnalyses = config.getObject("org.opalj.fpcf.registry.analyses")
            val entriesIterator = registeredAnalyses.entrySet.iterator
            while (entriesIterator.hasNext) {
                val entry = entriesIterator.next
                val id = entry.getKey
                val metaData = entry.getValue.asInstanceOf[ConfigObject]
                val description = metaData.getOrDefault("description", null).unwrapped.toString
                val default = metaData.getOrDefault("default", ConfigValueFactory.fromAnyRef(false))
                    .unwrapped().asInstanceOf[Boolean]

                val lazyFactory = metaData.getOrDefault("lazyFactory", null)
                if (lazyFactory ne null)
                    register(id, description, lazyFactory.unwrapped.toString, "lazy", default)

                val triggeredFactory = metaData.getOrDefault("triggeredFactory", null)
                if (triggeredFactory ne null)
                    register(
                        id,
                        description,
                        triggeredFactory.unwrapped.toString,
                        "triggered",
                        default && (lazyFactory eq null)
                    )

                val eagerFactory = metaData.getOrDefault("eagerFactory", null)
                if (eagerFactory ne null)
                    register(
                        id,
                        description,
                        eagerFactory.unwrapped.toString,
                        "eager",
                        default && (lazyFactory eq null) && (triggeredFactory eq null)
                    )
            }
        } catch {
            case e: Exception =>
                error("OPAL Setup", "registration of FPCF eager analyses failed", e)
        }
    }

    /**
     * Returns the ids of the registered analyses.
     */
    def analysisIDs(): Iterable[String] = this.synchronized {
        idToDescription.keys
    }

    /**
     * Returns the descriptions of the registered analyses. These descriptions are
     * expected to be useful to the end-users.
     */
    def analysisDescriptions(): Iterable[String] = this.synchronized {
        idToDescription.values
    }

    /**
     * Returns the current view of the registry for eager factories.
     */
    def eagerFactories: Iterable[FPCFEagerAnalysisScheduler] = this.synchronized {
        idToEagerScheduler.values
    }

    /**
     * Returns the current view of the registry for triggered factories.
     */
    def triggeredFactories: Iterable[FPCFTriggeredAnalysisScheduler] = this.synchronized {
        idToTriggeredScheduler.values
    }

    /**
     * Returns the current view of the registry for lazy factories.
     */
    def lazyFactories: Iterable[FPCFLazyAnalysisScheduler] = this.synchronized {
        idToLazyScheduler.values
    }

    /**
     * Returns the eager factory for analysis with a matching description.
     */
    def eagerFactory(id: String): FPCFEagerAnalysisScheduler = this.synchronized {
        idToEagerScheduler(id)
    }

    /**
     * Returns the triggered factory for analysis with a matching description.
     */
    def triggeredFactory(id: String): FPCFTriggeredAnalysisScheduler = this.synchronized {
        idToTriggeredScheduler(id)
    }

    /**
     * Returns the lazy factory for analysis with a matching description.
     */
    def lazyFactory(id: String): FPCFLazyAnalysisScheduler = this.synchronized {
        idToLazyScheduler(id)
    }

    /**
     * Returns the most suitable factory for analysis with a matching description.
     */
    def factory(id: String): FPCFAnalysisScheduler = this.synchronized {
        idToLazyScheduler.getOrElse(id, idToTriggeredScheduler.getOrElse(id, idToEagerScheduler(id)))
    }

    def defaultAnalysis(property: PropertyBounds): Option[ComputationSpecification[FPCFAnalysis]] = {
        propertyToDefaultScheduler.get(property)
    }

    registerFromConfig()

}
