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

import org.opalj.collection.immutable.Chain

/**
 * Encapsulates a computed schedule and enables the execution of it.
 *
 * @param batches The representation of the computed schedule.
 *
 * @author Michael Eichberg
 */
case class Schedule(
        batches: Chain[Chain[ComputationSpecification]]
) extends (PropertyStore ⇒ Unit) {

    /**
     * Schedules the computation specifications; that is, executes the underlying analysis scenario.
     *
     * @param ps The property store which should be used to execute the analyses.
     */
    def apply(ps: PropertyStore): Unit = {
        val initInfo =
            batches.flatMap { batch ⇒
                batch.toIterator.map[(ComputationSpecification, Any)] { cs ⇒ cs -> cs.init(ps) }
            }.toMap

        batches foreach { batch ⇒
            val computedProperties =
                batch.foldLeft(Set.empty[PropertyKind])((c, n) ⇒ c ++ n.derives)
            val openProperties =
                batches.dropWhile(_ ne batch).tail. // collect properties derived in the future
                    map(batch ⇒ batch.foldLeft(Set.empty[PropertyKind])((c, n) ⇒ c ++ n.derives)).
                    reduceOption((l, r) ⇒ l ++ r).
                    getOrElse(Set.empty)
            ps.setupPhase(computedProperties, openProperties)
            batch foreach { cs ⇒
                cs.beforeSchedule(ps)
                cs.schedule(ps, initInfo(cs).asInstanceOf[cs.InitializationData])
            }
            ps.waitOnPhaseCompletion()
            batch foreach { cs ⇒
                cs.afterPhaseCompletion(ps)
            }

        }
    }

    override def toString: String = {
        batches.map(_.map(_.name).mkString("{", ", ", "}")).
            mkString("Schedule(\n\t", "\n\t", "\n)")
    }

}
