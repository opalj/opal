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

case class SpecificationViolation(message: String) extends Exception(message)

/**
 * Specification of the properties of a fix-point computation (FPC) that are relevant
 * when computing the correct scheduling order.
 */
abstract class ComputationSpecification(
        val name:    String,
        val uses:    Set[PropertyKeyID],
        val derives: Set[PropertyKeyID]
) {

    if (derives.isEmpty) {
        throw new IllegalArgumentException("the computation does not derive any information")
    }

    /**
     * Called by the scheduler to start execution of this analysis.
     */
    def schedule(ps: PropertyStore): Unit

    override def toString: String = {
        val uses = this.uses.map(u ⇒ PropertyKey.name(u)).mkString("uses={", ", ", "}")
        val derives = this.derives.map(d ⇒ PropertyKey.name(d)).mkString("derives={", ", ", "}")
        s"FPC(name=$name,$uses,$derives)"
    }

}

class AnalysisScenario(
        private[this] var ccs: Set[ComputationSpecification] = Set.empty
) {

    def +=(cs: ComputationSpecification): Unit = this.synchronized {
        ccs += cs
    }

    /**
     * Returns the graph which depicts the dependencies between the properties based on
     * the selected computations. I.e., a property `d` depends on another property `p` if the
     * algorithm wich computes `d` uses the property `p`.
     */
    def propertyComputationsDependencies: Graph[PropertyKeyID] = {
        val psDeps = Graph.empty[PropertyKeyID]
        ccs foreach { cs ⇒
            // all derived properties depend on all used properties
            cs.derives foreach { derived ⇒
                psDeps += derived
                cs.uses foreach { use ⇒
                    psDeps += (derived, use)
                }
            }
        }
        psDeps
    }

    /**
     * Returns the dependencies between the computations.
     */
    def computationDependencies: Graph[ComputationSpecification] = {
        val compDeps = Graph.empty[ComputationSpecification]
        val derivedBy: Map[PropertyKeyID, ComputationSpecification] = {
            ccs.flatMap(cs ⇒ cs.derives map { derives ⇒ (derives, cs) }).toMap
        }
        ccs foreach { cs ⇒
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
        // 1. check that each property is derived by only one analysis
        var derived: Set[PropertyKeyID] = Set.empty
        var uses: Set[PropertyKeyID] = Set.empty
        ccs.foreach { cs ⇒
            cs.derives.foreach { pk ⇒
                if (derived.contains(pk)) {
                    val pkName = PropertyKey.name(pk)
                    val message = s"the property $pkName is derived by multiple analyses"
                    throw SpecificationViolation(message)
                } else {
                    derived += pk
                }
            }
            uses ++= cs.uses
        }
        // 2. check for properties that are not derived
        val underived = uses -- derived
        if (underived.nonEmpty) {
            val underivedInfo = underived.map(up ⇒ PropertyKey.name(up)).mkString(", ")
            val message = s"no analyses are scheduled for the properties: $underivedInfo"
            OPALLogger.warn("analysis configuration", message)
        }

        // 3. compute the schedule
        var batches: Chain[Chain[ComputationSpecification]] = Naught // MUTATED!!!!
        // Idea: to compute the batches we compute which properties are computed in which round.
        // 3.1. create dependency graph between analyses

        val computationDependencies = this.computationDependencies
        while (computationDependencies.nonEmpty) {
            var leafComputations = computationDependencies.leafNodes
            while (leafComputations.nonEmpty) {
                batches = batches ++! leafComputations.map(Chain.singleton(_)).to[Chain] // assign each computation to its own "batch"
                computationDependencies --= leafComputations
                leafComputations = computationDependencies.leafNodes
            }
            var cyclicComputations = closedSCCs(computationDependencies)
            while (cyclicComputations.nonEmpty) {
                val cyclicComputation = cyclicComputations.head
                cyclicComputations = cyclicComputations.tail
                batches = batches ++! (new :&:(cyclicComputation.to[Chain])) // assign cyclic computations to one batch
                computationDependencies --= cyclicComputation
            }
        }

        Schedule(batches)
    }

}

/**
 * Encapsulates a computed schedule and enables the execution of it.
 *
 * @param batches The representation of the computed schedule.
 */
case class Schedule(
        batches: Chain[Chain[ComputationSpecification]]
) extends ((PropertyStore) ⇒ Unit) {

    def apply(ps: PropertyStore): Unit = {
        batches.foreach { batch ⇒
            batch.foreach { fpc ⇒ fpc.schedule(ps) }
            ps.waitOnPropertyComputationCompletion(true)
        }
    }

    override def toString: String = {
        batches.map(_.map(_.name).mkString("{", ", ", "}")).mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}
