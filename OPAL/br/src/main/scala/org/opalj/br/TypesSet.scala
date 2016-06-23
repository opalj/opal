/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * ==Thread Safety==
 * This class is not thread safe.
 *
 * @author Michael Eichberg
 */
class TypesSet( final val classHierarchy: ClassHierarchy) {

    import classHierarchy.isSubtypeOf

    protected[this] var concreteTypes: Set[ObjectType] = Set.empty
    protected[this] var upperTypeBounds: Set[ObjectType] = Set.empty

    /**
     * Returns `true` if this set is empty.
     * @see [[size]]
     */
    def isEmpty: Boolean = concreteTypes.isEmpty && upperTypeBounds.isEmpty

    /**
     * Returns `true` if this set contains at least one type.
     * @see [[size]]
     */
    def nonEmpty: Boolean = concreteTypes.nonEmpty || upperTypeBounds.nonEmpty

    /**
     * The number of types explicitly stored in the set. This number is '''independent'''
     * of the number of represented types. E.g., if `java.lang.Object` is stored in this set
     * then the size of this set is 1 even though it represents all known types.
     */
    def size: Int = concreteTypes.size + upperTypeBounds.size

    def +=(tpe: ObjectType): Unit = {
        if (!concreteTypes.contains(tpe) &&
            !upperTypeBounds.exists(utb ⇒ isSubtypeOf(tpe, utb).isYes)) {
            concreteTypes += tpe
        }
    }

    def +<:=(tpe: ObjectType): Unit = {
        if (concreteTypes.contains(tpe)) {
            concreteTypes -= tpe
            upperTypeBounds = upperTypeBounds.filter(utb ⇒ isSubtypeOf(utb, tpe).isNoOrUnknown) + tpe
        } else {
            var doNotAddTPE: Boolean = false
            var newUpperTypeBounds = upperTypeBounds.filter { utb ⇒
                val keepExistingUTB = isSubtypeOf(utb, tpe).isNoOrUnknown
                if (keepExistingUTB && !doNotAddTPE && isSubtypeOf(tpe, utb).isYes) {
                    doNotAddTPE = true
                }
                keepExistingUTB
            }
            concreteTypes = concreteTypes.filter { ct ⇒ isSubtypeOf(ct, tpe).isNoOrUnknown }
            if (!doNotAddTPE) newUpperTypeBounds += tpe
            upperTypeBounds = newUpperTypeBounds
        }
    }

    /**
     * @param f A call back function will be called for each type stored in the set along with
     * 		the information if type represents an upper type bound (`true`) or refers to a
     * 		concrete class/interface type (the second parameter is then `false`).
     */
    def foreach[U](f: (ObjectType, Boolean) ⇒ U): Unit = {
        concreteTypes.foreach { tpe ⇒ f(tpe, false) }
        upperTypeBounds.foreach { tpe ⇒ f(tpe, true) }
    }

    /**
     * Returns a pair where the first set contains all concrete types and the second set
     * contains all upper type bounds.
     */
    def types: (Set[ObjectType], Set[ObjectType]) = (concreteTypes, upperTypeBounds)
}