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
 * Identifies those methods where the calling context can never be (completely) deduced from the
 * given code, because the methods may be called by some unknonwn code/code which necessarily
 * is outside of the scope of the current analysis.
 *
 * In general, the main methods of desktop applications are entry points, other examples are
 * the servlet methods; e.g, `doPost`. In the latter case, the servlet container guarantees
 * that the `HTTPServletRequest` object is non null, but the content is basically unconstrained
 * and no analysis should make any assumptions about it. In case of a library, all
 * public methods of all public types are entry points. Furthermore, protected methods of public,
 * extensible classes are also callable by unknown code and, hence, are also entry points.
 *
 * Entry points are modelled as properties, because an advanced analysis may only identify those
 * methods of objects as entry points that are actually callable by the end user, because the
 * end user can get access to an instance of a non-public class of the respective type.
 *
 * @author Michael Reif
 * @author Michael Eichberg
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
 *  The respective method is an entry point; the calling context is not completely known.
 */
case object IsEntryPoint extends EntryPoint { final def isRefineable: Boolean = false }

/**
 * It is not yet known if the respective method is an entry point or not.
 */
case object MayBeEntryPoint extends EntryPoint { final def isRefineable: Boolean = true }

/**
 * The respective method is not an entry point.
 */
case object NoEntryPoint extends EntryPoint { final def isRefineable: Boolean = false }
