package org.opalj.fpa.common

import org.opalj.fpa.FixpointAnalysis
import org.opalj.fpa.ShadowingAnalysis
import org.opalj.fpa.FactoryMethodAnalysis

/**
 * The fixpoint analyses registry is a registry for all analyses that need
 * to be computed with the fixpoint framework because they depend on other analyses.
 *
 * All registered analyses does compute a [[org.opalj.fp.Property]] associated with an [[org.opalj.fp.Entity]].
 * Instances for entities are, e.g.: ´classes´, ´methods´ or ´fields´.
 *
 * The registry was developed to support configuration purposes where the user/developer
 * choose between different domains.
 *
 * The analyses that are part of OPAL are already registered.
 *
 * @note The registry does not handle dependencies between analyses yet.
 *
 * ==Thread Safety==
 * The registry is thread safe.
 *
 * @author Michael Reif
 */
object FixpointAnalysesRegistry {

    private[this] var descriptions: Map[String, _ <: FixpointAnalysis] = Map.empty
    private[this] var theRegistry: Set[FixpointAnalysis] = Set.empty

    /**
     * Register a new fixpoint analysis that can be used to compute a property of an specific entity.
     *
     * @param analysisDescription A short description of the properties that the analysis computes; in
     *     particular w.r.t. a specific set of entities.
     * @param analysisClass The object of the analysis.
     */
    def register[FA <: FixpointAnalysis](
        analysisDescription: String,
        analysisClass: FA): Unit = {
        this.synchronized {
            descriptions += ((analysisDescription, analysisClass))
            theRegistry += analysisClass
        }

    }

    /**
     * Returns an `Iterable` to make it possible to iterate over the descriptions of
     * the analysis. Useful to show the (end-users) some meaningful descriptions.
     */
    def analysisDescriptions(): Iterable[String] = this.synchronized { descriptions.keys }

    /**
     * Returns the current view of the registry.
     */
    def registry: Set[FixpointAnalysis] = this.synchronized { theRegistry }

    /**
     * Return the [[FixpointAnalysis]] object that can be used to analyze a project later on.
     *
     * @Note This registry does only support scala `object`s. Fixpoint analyses implemented in an class
     * are currently not (directly) supported by the registry.
     */
    def newFixpointAnalysis(
        analysisDescripition: String): FixpointAnalysis = {
        this.synchronized {
            descriptions(analysisDescripition)
        }
    }

    // initialize the registry with the known default analyses

    register(
        "[ShadowingAnalysis] An analysis which computes the project accessibility of static melthods property w.r.t. clients.",
        ShadowingAnalysis
    )

    register(
        "[FactoryMethodAnalysis] An analysis which computes whether a static melthod is an accessible factory method w.r.t. clients.",
        FactoryMethodAnalysis
    )
}