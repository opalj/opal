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

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.log.OPALLogger

/**
 * A set of all allocation sites. 
 *
 * To initialize the set of allocation sites for an entire project use the respective
 * project information key: [[AllocationSitesKey]]. The key also provides further 
 * information regarding the concrete allocation site objects and their relation 
 * to the underlying method.
 *
 * @author Michael Eichberg
 */
class AllocationSites private[analyses] (val data: Map[Method, Map[PC, AllocationSite]]) {

    def apply(m: Method): Map[PC, AllocationSite] = data.getOrElse(m, Map.empty)

    def allocationSites: Iterable[AllocationSite] = data.values.flatMap(_.values)
}

/**
 * The ''key'' object to collect all allocation sites in a project. If the library methods
 * also contain bodies, the allocation sites for those methods are also made available.
 *
 * @note    An allocation site object is created for each `NEW` instruction found in a 
 *          method's body. However, this does not guarantee that the allocation site
 *          will ever be reached or is found in code derived from the method's bytecode.
 *          In particular the JDK is known for containing a lot of dead code 
 *          [[http://dl.acm.org/citation.cfm?id=2786865 Hidden Truths in Dead Software Paths]]
 *          and this code is automatically filtered when OPAL creates the 3-address code 
 *          representation. The default analysis done when creating the 3-address code is 
 *          basically equivalent to the one described in the above paper.
 *
 * @note    See [[org.opalj.br.AllocationSite]] for additional details.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object AllocationSitesKey extends ProjectInformationKey[AllocationSites, Nothing] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Collects all allocation sites.
     *
     * @note  This analysis is internally parallelized. I.e., it is advantageous to run this
     *        analysis in isolation.
     */
    override protected def compute(p: SomeProject): AllocationSites = {
        implicit val logContext = p.logContext
        val sites = new ConcurrentLinkedQueue[(Method, Map[PC, AllocationSite])]

        val errors = p.parForeachMethodWithBody(defaultIsInterrupted) { methodInfo ⇒
            val m = methodInfo.method
            val code = m.body.get
            val as = code.collectWithIndex {
                case (pc, instructions.NEW(_)) ⇒ (pc, new AllocationSite(m, pc))
            }.toMap
            if (as.nonEmpty) sites.add((m, as))
        }

        errors foreach { e ⇒
            OPALLogger.error("allocation sites", "collecting all allocation sites failed", e)
        }

        new AllocationSites(Map.empty ++ sites.asScala)
    }

    //
    // HELPER FUNCTION TO MAKE IT EASILY POSSIBLE TO ADD ALLOCATION SITES TO A PROPERTYSTORE
    // AND TO ENSURE THAT ALLOCATIONSITES AND THE PROPERTYSTORE CONTAIN THE SAME OBJECTS!
    //

    final val entityDerivationFunction: (SomeProject) ⇒ (Traversable[AnyRef], AllocationSites) =
        (p: SomeProject) ⇒ {
            // this will collect the allocations sites of the project if not yet collected...
            val allocationsSites = p.get(AllocationSitesKey)
            (allocationsSites.data.values.flatMap(_.values), allocationsSites)
        }
}
