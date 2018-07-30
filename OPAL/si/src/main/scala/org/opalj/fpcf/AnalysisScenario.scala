/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.graphs.closedSCCs
import org.opalj.graphs.Graph
import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.:&:

/**
 * Provides functionality to compute an optimal schedule to execute a set of analyses. Here,
 * optimal means that the schedule will try to minimize the number of notifications due to updated
 * properties. It will run analyses that just use information provided by earlier analyses,
 * but which do not provide information required by the earlier ones, in a later batch/phase.
 *
 * @author Michael Eichberg
 */
class AnalysisScenario {

    private[this] var allCS: Set[ComputationSpecification] = Set.empty
    private[this] var eagerCS: Set[ComputationSpecification] = Set.empty
    private[this] var lazyCS: Set[ComputationSpecification] = Set.empty
    private[this] var lazilyComputedProperties: Set[PropertyKind] = Set.empty

    def allProperties: Set[PropertyKind] = {
        eagerCS.foldLeft(Set.empty[PropertyKind]) { (c, n) ⇒ c ++ n.derives ++ n.uses } ++
            lazilyComputedProperties
    }

    def +=(cs: ComputationSpecification): Unit = this.synchronized {
        allCS += cs
        if (cs.isLazy) {
            // check that lazily computed properties are always only computed by ONE analysis
            cs.derives.find(lazilyComputedProperties.contains) foreach { p ⇒
                val m =
                    s"registration of $cs failed; "+
                        s"${PropertyKey.name(p.id)} is already computed by an analysis"
                throw new SpecificationViolation(m)
            }
            lazilyComputedProperties ++= cs.derives
            lazyCS += cs
        } else {
            eagerCS += cs
        }
    }

    /**
     * Returns the graph which depicts the dependencies between the computed properties
     * based on the current computation specifications.
     * I.e., a property `d` depends on another property `p` if the algorithm which computes
     * `d` uses the property `p`.
     */
    def propertyComputationsDependencies: Graph[PropertyKind] = {
        val psDeps = Graph.empty[PropertyKind]
        allCS foreach { cs ⇒
            // all derived properties depend on all used properties
            cs.derives foreach { derived ⇒
                psDeps += derived
                cs.uses foreach { use ⇒ psDeps += (derived, use) }
            }
        }
        psDeps
    }

    /**
     * Returns the dependencies between the computations.
     */
    def computationDependencies: Graph[ComputationSpecification] = {
        val compDeps = Graph.empty[ComputationSpecification]
        val derivedBy: Map[PropertyKind, Set[ComputationSpecification]] = {
            var derivedBy: Map[PropertyKind, Set[ComputationSpecification]] = Map.empty
            allCS foreach { cs ⇒
                cs.derives foreach { derives ⇒
                    derivedBy += derives -> (derivedBy.getOrElse(derives, Set.empty) + cs)
                }
            }
            derivedBy
        }
        allCS foreach { cs ⇒
            compDeps += cs
            cs.uses foreach { usedPK ⇒
                derivedBy.get(usedPK).iterator.flatten.foreach { providerCS ⇒
                    if (providerCS ne cs) {
                        compDeps += (cs, providerCS)
                    }
                }
            }
        }
        // let's handle the case that multiple analyses derives a property collaboratively
        derivedBy.valuesIterator.filter(_.size > 1) foreach { css ⇒
            val cssIt = css.iterator
            val headCS = cssIt.next()
            var lastCS = headCS
            do {
                val nextCS = cssIt.next()
                compDeps += (lastCS -> nextCS)
                lastCS = nextCS
            } while (cssIt.hasNext)
            compDeps += (lastCS -> headCS)
        }

        compDeps
    }

    /**
     * Computes a schedule. A schedule is a function that, given a property store, executes
     * the specified analyses.
     *
     * The goal is to find a schedule that:
     *   - ... schedules as many completely independent analyses in parallel as possible
     *   - ... does not schedule two analyses A and B at the same time if B has a dependency on
     *         the properties computed by A, but A has no dependency on B. Scheduling the
     *         computation of B in a later batch potentially minimizes the number of derivations.
     *   - ... schedules two analyses which collaboratively compute a property in the same batch/
     *         phase.
     */
    def computeSchedule(
        implicit
        logContext: LogContext
    ): Schedule = this.synchronized {
        if (eagerCS.isEmpty) {
            if (lazyCS.isEmpty) {
                return Schedule(Chain.empty)
            } else {
                // there is no need to compute batches...
                val scheduleBuilder = (Chain.newBuilder[ComputationSpecification] ++= lazyCS)
                return Schedule(Chain(scheduleBuilder.result))
            }
        }

        var derived: Set[PropertyKind] = Set.empty
        var uses: Set[PropertyKind] = Set.empty
        allCS foreach { cs ⇒
            cs.derives foreach { pk ⇒ derived += pk }
            uses ++= cs.uses
        }
        // 1. check for properties that are not derived
        val underived = uses -- derived
        if (underived.nonEmpty) {
            val underivedInfo = underived.iterator.map(up ⇒ PropertyKey.name(up)).mkString(", ")
            val message = s"no analyses are scheduled for the properties: $underivedInfo"
            OPALLogger.warn("analysis configuration", message)
        }

        // 2. compute the schedule
        var batches: Chain[Chain[ComputationSpecification]] = Naught // MUTATED!!!!
        // Idea: to compute the batches we compute which properties are computed in which round.
        // 2.1. create dependency graph between analyses

        val computationDependencies = this.computationDependencies
        while (computationDependencies.nonEmpty) {
            var leafComputations = computationDependencies.leafNodes
            while (leafComputations.nonEmpty) {
                // assign each computation to its own "batch"
                batches = batches ++! leafComputations.map(Chain.singleton).to[Chain]
                computationDependencies --= leafComputations
                leafComputations = computationDependencies.leafNodes
            }
            var cyclicComputations = closedSCCs(computationDependencies)
            while (cyclicComputations.nonEmpty) {
                val cyclicComputation = cyclicComputations.head
                cyclicComputations = cyclicComputations.tail
                // assign cyclic computations to one batch
                batches = batches ++! (new :&:(cyclicComputation.to[Chain]))
                computationDependencies --= cyclicComputation
            }
        }

        // 3. schedule all lazy computations in batch "0"
        if (lazyCS.nonEmpty) {
            val eagerBatches = batches.map(b ⇒ b filter { cs ⇒ !cs.isLazy }).filter(b ⇒ b.nonEmpty)
            val firstBatchWithLazyCS = (Chain.empty ++ lazyCS) :&:: eagerBatches.head
            Schedule(firstBatchWithLazyCS :&: eagerBatches.tail)
        } else {
            Schedule(batches)
        }
    }
}

/**
 * Factory to create an [[AnalysisScenario]].
 */
object AnalysisScenario {

    def apply(analyses: Set[ComputationSpecification]): AnalysisScenario = {
        val as = new AnalysisScenario
        analyses.foreach(as.+=)
        as
    }
}
