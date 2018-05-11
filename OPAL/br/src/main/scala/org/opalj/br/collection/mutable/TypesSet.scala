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
package collection
package mutable

import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * ==Thread Safety==
 * This class is not thread safe.
 *
 * @author Michael Eichberg
 */
class TypesSet( final val classHierarchy: ClassHierarchy) extends collection.TypesSet {

    import classHierarchy.isSubtypeOf

    protected[this] var theConcreteTypes: Set[ObjectType] = Set.empty
    protected[this] var theUpperTypeBounds: Set[ObjectType] = Set.empty

    /**
     * The set of concrete types which are not subtypes of any type which
     * is returned by `upperTypeBounds`.
     */
    final def concreteTypes: Set[ObjectType] = theConcreteTypes
    final def upperTypeBounds: Set[ObjectType] = theUpperTypeBounds

    def toImmutableTypesSet: immutable.TypesSet =
        immutable.TypesSet(theConcreteTypes, theUpperTypeBounds)(classHierarchy)

    def +=(tpe: ObjectType): Unit = {
        if (!theConcreteTypes.contains(tpe) &&
            !theUpperTypeBounds.exists(utb ⇒ isSubtypeOf(tpe, utb).isYes)) {
            theConcreteTypes += tpe
        }
    }

    def ++=(tpes: Traversable[ObjectType]): Unit = tpes.foreach { += }

    def ++<:=(tpes: Traversable[ObjectType]): Unit = tpes.foreach { +<:= }

    /**
     * Adds the given upper type bound to this `TypesSet` unless a supertype
     * of the given type is already added as an upper type bound.
     *
     * All subtypes – whether concrete or upper types bounds – are removed.
     */
    def +<:=(tpe: ObjectType): Unit = {
        if (theConcreteTypes.contains(tpe)) {
            theConcreteTypes -= tpe
            theUpperTypeBounds =
                theUpperTypeBounds.filter(utb ⇒ isSubtypeOf(utb, tpe).isNoOrUnknown) + tpe
        } else {
            var doNotAddTPE: Boolean = false
            var newUpperTypeBounds = theUpperTypeBounds.filter { utb ⇒
                val keepExistingUTB = isSubtypeOf(utb, tpe).isNoOrUnknown
                if (keepExistingUTB && !doNotAddTPE && isSubtypeOf(tpe, utb).isYes) {
                    doNotAddTPE = true
                }
                keepExistingUTB
            }
            theConcreteTypes = theConcreteTypes.filter { ct ⇒ isSubtypeOf(ct, tpe).isNoOrUnknown }
            if (!doNotAddTPE) newUpperTypeBounds += tpe
            theUpperTypeBounds = newUpperTypeBounds
        }
    }
}
