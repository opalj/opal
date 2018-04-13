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

/**
 * An efficient representation of a set of types if some types are actually upper type bounds
 * and hence already represent sets of types.
 *
 * @author Michael Eichberg
 */
abstract class TypesSet /*extends Set[(ObjectType,...)]*/ {

    def concreteTypes: Set[ObjectType] // IMPROVE [L2] Use UIDSet
    def upperTypeBounds: Set[ObjectType] // IMPROVE [L2] Use UIDSet

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

    /**
     * @param f A call back function will be called for each type stored in the set along with
     *      the information if type represents an upper type bound (`true`) or refers to a
     *      concrete class/interface type (the second parameter is then `false`).
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

    final override def equals(other: Any): Boolean = {

        other match {
            case that: TypesSet ⇒
                concreteTypes == that.concreteTypes && upperTypeBounds == that.upperTypeBounds
            case _ ⇒ false
        }
    }

    final override lazy val hashCode: Int = {
        concreteTypes.hashCode() * 111 + upperTypeBounds.hashCode()
    }

    override def toString: String = {
        upperTypeBounds.map(_.toJava).mkString(
            concreteTypes.map(_.toJava).mkString(
                "TypesSet(concreteTypes={",
                ",",
                "},upperTypeBounds={"
            ),
            ",",
            "})"
        )
    }
}

object TypesSet {

    def empty: EmptyTypesSet.type = EmptyTypesSet

    final val SomeException: TypesSet = UpperTypeBounds(Set(ObjectType.Throwable))
}

case object EmptyTypesSet extends TypesSet {

    def concreteTypes: Set[ObjectType] = Set.empty
    def upperTypeBounds: Set[ObjectType] = Set.empty

}

case class TheTypes( final val concreteTypes: Set[ObjectType]) extends TypesSet {

    final override def upperTypeBounds = Set.empty

}

case class UpperTypeBounds( final val upperTypeBounds: Set[ObjectType]) extends TypesSet {

    final override def concreteTypes = Set.empty

}
