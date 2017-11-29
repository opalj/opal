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
package br
package analyses

import scala.collection.AbstractIterator
import scala.collection.AbstractIterable

import org.opalj.collection.immutable.ConstArray

/**
 * A set of allocation sites. (Typically, a [[Project]]'s set of allocation sites.)
 *
 * To initialize the set of allocation sites for an entire project use the respective
 * project information key: [[AllocationSitesKey]]. The key also provides further
 * information regarding the concrete allocation site objects and their relation
 * to the underlying method.
 *
 * @param  allocationsByType All allocation site of a specific reference type. Note, that
 *         the value is effectively an array which does not support an efficient contains check!
 *
 * @author Michael Eichberg
 * @author Florian Kübler
 */
class AllocationSites private[analyses] (
        val allocationsPerMethod: Map[Method, Map[PC, AllocationSite]],
        val allocationsByType:    Map[ReferenceType, ConstArray[AllocationSite]]
) extends AbstractIterable[AllocationSite] {

    // let's check if the data is as expected
    assert(allocationsByType.valuesIterator.forall(_.nonEmpty))
    assert(allocationsPerMethod.valuesIterator.forall(_.nonEmpty))
    // let's check the inner consistency of the data ... it works, but it takes ages(!!!)
    // assert(
    //     allocationsPerMethod.values.forall(_.values.forall { as ⇒
    //        allocationsByType(as.allocatedType).contains(as)
    //    })
    // )

    def apply(m: Method): Map[PC, AllocationSite] = allocationsPerMethod.getOrElse(m, Map.empty)

    def apply(t: ReferenceType): ConstArray[AllocationSite] = {
        allocationsByType.getOrElse(t, ConstArray.empty)
    }

    def iterator: Iterator[AllocationSite] = {
        new AbstractIterator[AllocationSite] {

            private val typeBasedIterator: Iterator[ConstArray[AllocationSite]] = {
                allocationsByType.values.iterator
            }

            private var siteBasedIterator: Iterator[AllocationSite] = null

            def hasNext: Boolean = {
                typeBasedIterator.hasNext ||
                    (siteBasedIterator != null && siteBasedIterator.hasNext)
            }

            def next: AllocationSite = {
                if (siteBasedIterator == null || !siteBasedIterator.hasNext) {
                    siteBasedIterator = typeBasedIterator.next.toIterator
                }
                siteBasedIterator.next
            }
        }
    }
}
