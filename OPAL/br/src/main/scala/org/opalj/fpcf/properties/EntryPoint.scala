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
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey

/**
 * The common super trait of all entry point properties.
 */
sealed trait EntryPoint extends Property {

    final type Self = EntryPoint

    final def key = EntryPoint.Key

    final def isRefinable = false
}

object EntryPoint {

    final val cycleResolutionStrategy: PropertyKey.CycleResolutionStrategy = (
        ps: PropertyStore,
        epks: PropertyKey.SomeEPKs
    ) ⇒ {
        throw new Error("there should be no cycles") //TODO: fill in CycleResolution Strategy
    }

    final val Key = {
        PropertyKey.create[EntryPoint](
            "EntryPoint",
            fallbackProperty = (ps: PropertyStore, e: Entity) ⇒ IsEntryPoint,
            cycleResolutionStrategy = cycleResolutionStrategy
        )

    }
}

/**
 * The respective method is an entry point.
 */
case object IsEntryPoint extends EntryPoint { final val isRefineable: Boolean = false }

/**
 * The respective method is not an entry point.
 */
case object NoEntryPoint extends EntryPoint { final val isRefineable: Boolean = false }
