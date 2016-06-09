/**
 * BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey

/**
 * Determines for each method if it potentially can be directly called by a client.
 * This can happens if:
 * - the method is directly visible to the client
 * - the method can become visible by a visible subclass which inherits the respective method
 * - the method can become pseudo-visible by a call on a superclass/interface (if the class is upcasted)
 *
 * @note This property is computed on-demand by a direct property computation.
 * @author Michael Reif
 */
sealed trait ClientCallable extends Property {
    final type Self = ClientCallable

    final def key = ClientCallable.Key

    final def isRefineable = false
}

object ClientCallable {

    final val cycleResolutionStrategy: PropertyKey.CycleResolutionStrategy = (
        ps: PropertyStore,
        epks: PropertyKey.SomeEPKs
    ) ⇒ {
        //TODO fill in cycle resolution strategy
        throw new Error("there should be no cycles")
    }

    final val Key = {
        PropertyKey.create[ClientCallable](
            "ClientCallable",
            fallbackProperty = (ps: PropertyStore, e: Entity) ⇒ IsClientCallable,
            cycleResolutionStrategy = cycleResolutionStrategy
        )
    }
}

case object IsClientCallable extends ClientCallable

case object NotClientCallable extends ClientCallable