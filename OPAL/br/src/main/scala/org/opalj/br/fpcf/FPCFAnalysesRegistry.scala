/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import scala.reflect.runtime.universe.runtimeMirror

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
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
    private[this] var idToDescription: Map[String, String] = Map.empty

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
        lazyFactory:         Boolean
    ): Unit = this.synchronized {
        val analysisRunner = resolveAnalysisRunner(analysisFactory)
        if (analysisRunner.nonEmpty) {
            if (lazyFactory) idToLazyScheduler +=
                ((analysisID, analysisRunner.get.asInstanceOf[FPCFLazyAnalysisScheduler]))
            else idToEagerScheduler +=
                ((analysisID, analysisRunner.get.asInstanceOf[FPCFEagerAnalysisScheduler]))

            idToDescription += ((analysisID, analysisDescription))
            val analysisType = if (lazyFactory) "lazy" else "eager"
            val message =
                s"registered $analysisType analysis: $analysisID ($analysisDescription)"
            info("OPAL Setup", message)
        } else {
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

    private[this] case class AnalysisFactory(id: String, description: String, factory: String)

    def registerFromConfig(): Unit = {
        val config = ConfigFactory.load()
        try {
            /* HANDLING FOR AN ARRAY OF ANALYSES... analyses = [ {},...,{}]
            //import com.typesafe.config.Config
            //import net.ceedubs.ficus.Ficus._
            //import net.ceedubs.ficus.readers.ArbitraryTypeReader._
            val key = "org.opalj.fpcf.registry.analyses"
            val registeredAnalyses = config.as[List[AnalysisFactory]](key)
            registeredAnalyses foreach { a =>
                register(a.id, a.description, a.factory)
            }
            */
            val registeredAnalyses = config.getObject("org.opalj.fpcf.registry.analyses")
            val entriesIterator = registeredAnalyses.entrySet.iterator
            while (entriesIterator.hasNext) {
                val entry = entriesIterator.next
                val id = entry.getKey
                val metaData = entry.getValue.asInstanceOf[ConfigObject]
                val description = metaData.getOrDefault("description", null).unwrapped.toString
                val eagerFactory = metaData.getOrDefault("eagerFactory", null)
                if (eagerFactory ne null)
                    register(id, description, eagerFactory.unwrapped.toString, lazyFactory = false)
                val lazyFactory = metaData.getOrDefault("lazyFactory", null)
                if (lazyFactory ne null)
                    register(id, description, lazyFactory.unwrapped.toString, lazyFactory = true)
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
     * Returns the lazy factory for analysis with a matching description.
     */
    def lazyFactory(id: String): FPCFLazyAnalysisScheduler = this.synchronized {
        idToLazyScheduler(id)
    }

    registerFromConfig()

}
