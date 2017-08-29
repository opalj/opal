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
package org.opalj.fpcf

import java.util.concurrent.CountDownLatch

/**
 * An information associated with an entity. Each property belongs to exactly one
 * property kind identified by a [[PropertyKey]]. Furthermore, each property
 * is associated with at most one property per property kind.
 *
 * ==Implementation Requirements==
 * Each implementation of the property trait has to implement an `equals` method that
 * determines if two properties are equal.
 *
 * @author Michael Eichberg
 */
trait Property extends PropertyMetaInformation {

    /**
     * Returns `true` if the current property may be refined in the future and it is therefore
     * necessary to wait for updates.
     */
    def isRefineable: Boolean

    /**
     *  Returns `true` if this property is always final and no refinement is possible.
     */
    final def isFinal: Boolean = !isRefineable

    /**
     * Equality of Properties has to be based on structural equality!
     */
    override def equals(other: Any): Boolean

    /**
     * Returns true if this property is currently computed or if its computation is already
     * scheduled.
     */
    // only used in combination with direct property computations
    private[fpcf] def isBeingComputed: Boolean = false

    /**
     * Returns true if this property inherits from [[OrderedProperty]].
     */
    private[fpcf] def isOrdered: Boolean = false

    /**
     * Returns `this` if this property inherits from [[OrderedProperty]].
     *
     * Used by the framework for debugging purposes only!
     */
    private[fpcf] def asOrderedProperty: OrderedProperty = {
        throw new ClassCastException(s"$this is not an OrderedProperty")
    }

}

//
//
// FRAMEWORK INTERNAL PROPERTIES
//
//

private[fpcf] trait PropertyIsBeingComputed extends Property {

    final override def key: Nothing = throw new UnsupportedOperationException
    final override def isRefineable: Nothing = throw new UnsupportedOperationException
    final override private[fpcf] def isBeingComputed: Boolean = true

}

private[fpcf] object PropertyIsBeingComputed {

    def unapply(p: Property): Boolean = (p ne null) && p.isBeingComputed

}

/**
 * A property that is used to state that the property is currently computed as part of a
 * direct property computation.
 *
 * This property is used to synchronize access to the property if the property
 * is computed using a direct property computation; in this case the first process - which also
 * performs the computation - decrements the CountDownLatch once the computation has succeeded.
 * All other processes just wait until the CountDownLatch is decremented.
 *
 * Recall that a direct property computation is executed by the thread that queries the thread and
 * that a direct property computation is always only allowed to depend on either previously
 * computed properties or properties whose computation must not have a dependency on the currently
 * computed property.
 */
private[fpcf] final class PropertyIsDirectlyComputed
    extends CountDownLatch(1)
    with PropertyIsBeingComputed {

    type Self = PropertyIsDirectlyComputed

}

private[fpcf] case object PropertyIsLazilyComputed extends PropertyIsBeingComputed {

    type Self = PropertyIsLazilyComputed.type

}
