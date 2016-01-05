/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package analysis
package methods

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

/**
 * Common supertrait of all purity properties.
 */
sealed trait Purity extends Property {

    /**
     * Returns the key used by all `Purity` properties.
     */
    // All instances have to share the SAME key!
    final def key = Purity.key

}
/**
 * Common constants use by all [[Purity]] properties associated with methods.
 */
object Purity extends PropertyMetaInformation {

    /**
     * The key associated with every purity property.
     */
    final val key =
        PropertyKey.create(
            // The unique name of the property.
            "Purity",
            // The default property that will be used if no analysis is able
            // to (directly) compute the respective property.
            MaybePure,
            // When we have a cycle all properties are necessarily conditionally pure
            // hence, we can leverage the "pureness" 
            Pure
        // NOTE
        // We DON NOT increase the pureness of all methods as this will happen automatically
        // as a sideeffect of setting the pureness of one method!
        // (epks: Iterable[EPK]) ⇒ { epks.map(epk ⇒ Result(epk.e, Pure)) }
        )
}

/**
 * The fallback/default purity.
 *
 * It is only used by the framework in case of a dependency
 * on an element for which no result could be computed.
 */
case object MaybePure extends Purity { final val isRefineable = true }

/**
 * Used if we know that the pureness of a methods only depends on the pureness
 * of the target methods.
 *
 * A conditionally pure method has to be treated as an inpure methods by clients
 * except that it may be refined later on.
 */
case object ConditionallyPure extends Purity { final val isRefineable = true }

/**
 * The respective method is pure.
 */
case object Pure extends Purity { final val isRefineable = false }

/**
 * The respective method is impure.
 */
case object Impure extends Purity { final val isRefineable = false }

