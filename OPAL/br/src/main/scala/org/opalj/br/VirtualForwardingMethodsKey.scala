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

import scala.collection.JavaConverters._
import java.util.concurrent.ConcurrentLinkedQueue

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.log.OPALLogger.error

/**
 * The ''key'' object to get information about all virtual forwarding methods.
 *
 * @note See [[org.opalj.br.VirtualForwardingMethod]] for further details.
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
 *          `this` object.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
object VirtualForwardingMethodsKey
    extends ProjectInformationKey[Traversable[VirtualForwardingMethod], Nothing] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Collects all virtual forwarding methods.
     *
     * @note This analysis is internally parallelized.
     */
    override protected def compute(p: SomeProject): Traversable[VirtualForwardingMethod] = {
        implicit val logContext = p.logContext

        val sites = new ConcurrentLinkedQueue[VirtualForwardingMethod]

        val errors = p.parForeachClassFile() { cf ⇒
            for {
                // all instance methods present in the current class file, including methods derived
                // from any supertype that are not overriden by this type.
                mc ← p.instanceMethods(cf.thisType)
                m = mc.method
                md = m.descriptor
                vm = VirtualForwardingMethod(cf.thisType, m.name, md, m)
            } {
                sites.add(vm)
            }
        }
        errors.foreach { e ⇒
            error("virtual forwarding methods", "collecting virtual forwarding methods failed", e)
        }

        sites.asScala
    }

    //
    // HELPER FUNCTION TO MAKE IT EASILY POSSIBLE TO ADD VIRTUAL FORWARDING METHODS TO A
    // PROPERTYSTORE!
    //
    final val entityDerivationFunction: (SomeProject) ⇒ (Traversable[AnyRef], Traversable[VirtualForwardingMethod]) = {
        (p: SomeProject) ⇒
            {
                val virtualMethods = p.get(VirtualForwardingMethodsKey)
                (virtualMethods, virtualMethods)
            }
    }
}
