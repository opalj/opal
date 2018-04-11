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

/**
 * An information associated with an entity. Each property belongs to exactly one
 * property kind specified by the [[PropertyKey]]. Furthermore, each property
 * is associated with at most one property per property kind.
 *
 * ==Implementation Requirements==
 *
 * ===Structural Equality===
 * Each implementation of the property trait has to implement an `equals` method that
 * determines if two properties are equal.
 *
 * @author Michael Eichberg
 */
trait Property extends PropertyMetaInformation {

    /**
     * Equality of properties has to be based on structural equality!
     */
    override def equals(other: Any): Boolean

    //
    //
    // IMPLEMENTATION PRIVATE METHODS
    //
    //

    /**
     * Returns `true` if this property is currently computed or if its computation is already
     * scheduled.
     */
    private[fpcf] def isBeingComputed: Boolean = false

    /**
     * Returns `true` if this property inherits from [[OrderedProperty]].
     */
    final def isOrderedProperty: Boolean = this.isInstanceOf[OrderedProperty]

    /**
     * Returns `this` if this property inherits from [[OrderedProperty]].
     *
     * Used, e.g., by the framework to support debugging analyses.
     */
    final def asOrderedProperty: OrderedProperty = this.asInstanceOf[OrderedProperty]

}

//
//
// FRAMEWORK INTERNAL PROPERTIES
//
//

private[fpcf] case object PropertyIsLazilyComputed extends Property {

    type Self = PropertyIsLazilyComputed.type

    final override def key: Nothing = throw new UnsupportedOperationException

    final override private[fpcf] def isBeingComputed: Boolean = true

}

