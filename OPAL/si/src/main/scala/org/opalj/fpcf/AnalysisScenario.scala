/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
 * properties by running analyses that just use information provided by earlier analyses,
 * but which do not provide information required by the earlier ones, in a later batch.
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
        val derivedBy: Map[PropertyKind, ComputationSpecification] = {
            allCS.flatMap(cs ⇒ cs.derives.map(derives ⇒ (derives, cs))).toMap
        }
        allCS foreach { cs ⇒
            compDeps += cs
            cs.uses foreach { usedPK ⇒
                derivedBy.get(usedPK).map { providerCS ⇒
                    if (providerCS ne cs) {
                        compDeps += (cs, providerCS)
                    }
                }
            }
        }
        compDeps
    }

    /**
     * Computes a schedule. A schedule is a function that, given a property store, executes
     * the specified analyses.
     *
     * The goal is to find a schedule that:
     *   -  ... schedules as many analyses in parallel as possible
     *   -  ... does not schedule two analyses A and B at the same time if B has a dependency on
     *          the properties computed by A, but A has no dependency on B. Scheduling the
     *          computation of B in a later batch potentially minimizes the number of derivations.
     *
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

object AnalysisScenario {

    def apply(analyses: Set[ComputationSpecification]): AnalysisScenario = {
        val as = new AnalysisScenario
        analyses.foreach(as.+=)
        as
    }
}
