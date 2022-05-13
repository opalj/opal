/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.graphs.Graph

/**
 * Provides functionality to determine whether a set of analyses is compatible and to compute
 * a schedule to execute a set of analyses.
 *
 * @author Michael Eichberg
 */
class AnalysisScenario[A](val ps: PropertyStore) {

    private[this] var scheduleComputed: Boolean = false

    private[this] var allCS: Set[ComputationSpecification[A]] = Set.empty

    private[this] var derivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var eagerlyDerivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var collaborativelyDerivedProperties: Set[PropertyBounds] = Set.empty
    private[this] var lazilyDerivedProperties: Set[PropertyBounds] = Set.empty

    private[this] var initializationData: Map[ComputationSpecification[A], Any] = Map.empty

    private[this] var usedProperties: Set[PropertyBounds] = Set.empty

    private[this] var eagerCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var lazyCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var triggeredCS: Set[ComputationSpecification[A]] = Set.empty
    private[this] var transformersCS: Set[ComputationSpecification[A]] = Set.empty

    private[this] var derivedBy: Map[PropertyKind, (PropertyBounds, Set[ComputationSpecification[A]])] = {
        Map.empty
    }

    def allProperties: Set[PropertyBounds] = derivedProperties ++ usedProperties

    /**
     * Adds the given computation specification (`cs`) to the set of computation specifications
     * that should be scheduled.
     */
    def +=(cs: ComputationSpecification[A]): this.type = {
        if (scheduleComputed) {
            throw new IllegalStateException("process was already computed");
        }

        cs.computationType match {
            case EagerComputation     => eagerCS += cs
            case TriggeredComputation => triggeredCS += cs
            case LazyComputation      => lazyCS += cs
            case Transformer          => transformersCS += cs
        }

        allCS += cs

        initializationData += cs -> cs.init(ps)

        this
    }

    /**
     * Returns the graph which depicts the dependencies between the computed properties
     * based on the current computation specifications.
     * I.e., a property `d` depends on another property `p` if the algorithm which computes
     * `d` uses the property `p`.
     *
     * @note Can only be called after the schedule method was called! If no schedule could be
     *       computed the result of this method is undefined.
     */
    def propertyComputationsDependencies: Graph[PropertyBounds] = {
        if (!scheduleComputed) {
            throw new IllegalStateException("initialization incomplete; schedule not computed");
        }
        val psDeps = Graph.empty[PropertyBounds]
        allCS foreach { cs =>
            // all derived properties depend on all used properties
            cs.derives foreach { derived =>
                psDeps addVertice derived
                cs.uses(ps) foreach { use => psDeps addEdge (derived, use) }
            }
        }
        psDeps
    }

    /**
     * Returns the dependencies between the computations.
     *
     * @note Can only be called after the schedule method was called! If no schedule could be
     *       computed the result of this method is undefined.
     */
    def computationDependencies: Graph[ComputationSpecification[A]] = {
        if (!scheduleComputed) {
            throw new IllegalStateException("initialization incomplete; schedule not computed");
        }

        val compDeps = Graph.empty[ComputationSpecification[A]]
        val derivedBy: Map[PropertyBounds, Set[ComputationSpecification[A]]] = {
            var derivedBy: Map[PropertyBounds, Set[ComputationSpecification[A]]] = Map.empty
            allCS foreach { cs =>
                cs.derives foreach { derives =>
                    derivedBy += derives -> (derivedBy.getOrElse(derives, Set.empty) + cs)
                }
            }
            derivedBy
        }
        allCS foreach { cs =>
            compDeps addVertice cs
            cs.uses(ps) foreach { usedPK =>
                derivedBy.get(usedPK).iterator.flatten.foreach { providerCS =>
                    if (providerCS ne cs) {
                        compDeps addEdge (cs, providerCS)
                    }
                }
            }
        }
        // let's handle the case that multiple analyses derive a property collaboratively
        derivedBy.valuesIterator.filter(_.size > 1) foreach { css =>
            val cssIt = css.iterator
            val headCS = cssIt.next()
            var lastCS = headCS
            do {
                val nextCS = cssIt.next()
                compDeps addEdge (lastCS -> nextCS)
                lastCS = nextCS
            } while (cssIt.hasNext)
            compDeps addEdge (lastCS -> headCS)
        }

        compDeps
    }

    private[this] def processCS(cs: ComputationSpecification[A]): Unit = {
        // 1. check the most basic constraints
        cs.derivesLazily foreach { lazilyDerivedProperty =>
            if (derivedProperties.contains(lazilyDerivedProperty)) {
                val pkName = PropertyKey.name(lazilyDerivedProperty.pk.id)
                val m = s"can not register $cs: $pkName is already computed by another analysis"
                throw new SpecificationViolation(m)
            }
        }

        cs.derivesCollaboratively foreach { collaborativelyDerivedProperty =>
            if (eagerlyDerivedProperties.contains(collaborativelyDerivedProperty) ||
                lazilyDerivedProperties.contains(collaborativelyDerivedProperty)) {
                val pkName = PropertyKey.name(collaborativelyDerivedProperty.pk.id)
                val m =
                    s"can not register $cs: "+
                        s"$pkName is not computed collaboratively by all analyses"
                throw new SpecificationViolation(m)
            }
        }

        cs.derivesEagerly foreach { eagerlyDerivedProperty =>
            if (derivedProperties.contains(eagerlyDerivedProperty)) {
                val pkName = PropertyKey.name(eagerlyDerivedProperty.pk.id)
                val m = s"can not register $cs: $pkName is already computed by another analysis"
                throw new SpecificationViolation(m)
            }
        }

        // TODO Check inner consistency: that is, if an analysis derives multiple properties, they have to be compatible

        // 2. register the analysis
        def handleDerivedProperties(derivedProperties: Set[PropertyBounds]): Unit = {
            this.derivedProperties ++= derivedProperties
            derivedProperties foreach { derivedProperty =>
                val pk = derivedProperty.pk
                derivedBy.get(pk) match {
                    case None =>
                        derivedBy += ((pk, (derivedProperty, Set(cs))))
                    case Some((`derivedProperty`, css)) =>
                        derivedBy += ((pk, (derivedProperty, css + cs)))
                    case Some((deviatingPropertyBounds, css)) =>
                        val propertyName = PropertyKey.name(pk)
                        throw new IllegalArgumentException(
                            s"different bounds ($deviatingPropertyBounds vs. $derivedProperty) "+
                                s"are computed by $css vs. $cs "+
                                s"for the collaboratively computed property $propertyName"
                        )
                }
            }
        }

        usedProperties ++= cs.uses(ps)

        eagerlyDerivedProperties ++= cs.derivesEagerly
        handleDerivedProperties(cs.derivesEagerly)
        collaborativelyDerivedProperties ++= cs.derivesCollaboratively
        handleDerivedProperties(cs.derivesCollaboratively)
        lazilyDerivedProperties ++= cs.derivesLazily.toList
        handleDerivedProperties(cs.derivesLazily.toSet)
    }

    /**
     * Computes an executable schedule.
     *
     * When executing a schedule, the following steps will be performed:
     *  1. all analyses' init method will be called; this may lead to the initialization
     *     of properties
     *
     *  1. setupPhase is called
     *  1. all lazy analyses and all transformers are registered;
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. all triggered computations are registered; this may trigger computations related
     *     to initial properties
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. all eager analyses are started
     *     (Immediately before registration, beforeSchedule is called.)
     *  1. when the phase has finished, all analyses' afterPhaseCompletion methods are called.
     *
     * @param propertyStore required to determine which properties are already computed!
     */
    def computeSchedule(
        propertyStore: PropertyStore
    )(
        implicit
        logContext: LogContext
    ): Schedule[A] = {
        if (scheduleComputed) {
            throw new IllegalStateException("schedule already computed");
        } else {
            scheduleComputed = true
        }

        allCS.foreach(processCS)

        val alreadyComputedPropertyKinds = propertyStore.alreadyComputedPropertyKindIds.toSet

        // 0. check that a property was not already derived
        allCS.foreach { cs =>
            cs.derives foreach { derivedProperty =>
                if (alreadyComputedPropertyKinds.contains(derivedProperty.pk.id)) {
                    val pkName = PropertyKey.name(derivedProperty.pk.id)
                    val m = s"can not register $cs: $pkName was computed in a previous phase"
                    throw new SpecificationViolation(m)
                }
            }
        }

        // 1. check for properties that are not derived (and which require an analysis)
        val underivedProperties = usedProperties -- derivedProperties
        underivedProperties
            .filterNot { underivedProperty =>
                alreadyComputedPropertyKinds.contains(underivedProperty.pk.id)
            }
            .foreach { underivedProperty =>
                val propertyName = PropertyKey.name(underivedProperty.pk.id)
                if (PropertyKey.hasFallback(underivedProperty.pk)) {
                    val message = s"no analyses scheduled for: $propertyName; using fallback"
                    OPALLogger.warn("analysis configuration", message)
                } else {
                    throw new IllegalStateException(s"no analysis scheduled for $propertyName")
                }
            }

        // TODO check all further constraints (in particular those related to cyclic dependencies between analysis...)

        // 2. assign analyses to different batches if an analysis can only process
        //    final properties (unless it is a transformer, the latter have special paths and
        //    constraints and can always be scheduled in the same batch!)

        // TODO ....

        Schedule(
            if (allCS.isEmpty) List.empty else List(computePhase(propertyStore)),
            initializationData
        )
    }

    /**
     * Computes the configuration for a specific batch; this method can only handle the situation
     * where all analyses can be executed in the same phase.
     */
    private def computePhase(propertyStore: PropertyStore): PhaseConfiguration[A] = {

        // 1. compute the phase configuration; i.e., find those properties for which we must
        //    suppress interim updates.
        var suppressInterimUpdates: Map[PropertyKind, Set[PropertyKind]] = Map.empty
        // Interim updates have to be suppressed when an analysis uses a property for which
        // the wrong bounds/not enough bounds are computed.
        transformersCS foreach { cs =>
            suppressInterimUpdates += (cs.derivesLazily.get.pk -> cs.uses(ps).map(_.pk))
        }

        // 3. create the batch
        val batchBuilder = List.newBuilder[ComputationSpecification[A]]
        batchBuilder ++= lazyCS
        batchBuilder ++= transformersCS
        batchBuilder ++= triggeredCS
        batchBuilder ++= eagerCS

        // FIXME...

        // Interim updates can be suppressed when the depender and dependee are not in a cyclic
        // relation; however, this could have a negative impact on the effect of deep laziness -
        // once we are actually implementing it. For the time being, suppress notifications is always
        // advantageous.

        val phase1Configuration = PropertyKindsConfiguration(
            propertyKindsComputedInThisPhase = derivedProperties.map(_.pk),
            suppressInterimUpdates = suppressInterimUpdates
        )

        PhaseConfiguration(phase1Configuration, batchBuilder.result())
    }
}

/**
 * Factory to create an [[AnalysisScenario]].
 */
object AnalysisScenario {

    /**
     * @param analyses The set of analyses that should be executed as part of this analysis scenario.
     */
    def apply[A](
        analyses:      Iterable[ComputationSpecification[A]],
        propertyStore: PropertyStore
    ): AnalysisScenario[A] = {
        val as = new AnalysisScenario[A](propertyStore)
        analyses.foreach(as.+=)
        as
    }

}
