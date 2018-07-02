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

import org.opalj.collection.immutable.ConstArray

/**
 * Represents a declared method of a class identified by [[declaringClassType]];
 * that is, a method which belongs to the API of the class itself or a super class thereof.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
sealed abstract class DeclaredMethod {

    /**
     * The declaring type; the returned type may not define the method; it could be defined by
     * one or more super classes/interfaces in case of Java 8+.
     */
    def declaringClassType: ReferenceType

    def name: String

    def descriptor: MethodDescriptor

    def toJava: String = s"${declaringClassType.toJava}{ ${descriptor.toJava(name)} }"

    /**
     * If `true`, the method which actually defines this method (which may still be abstract!),
     * is unique, known and is available using [[asDefinedMethod]].
     */
    def hasSingleDefinedMethod: Boolean

    /** The definition of this method; defined iff [[hasSingleDefinedMethod]] returns `true`. */
    def asDefinedMethod: DefinedMethod

    /**
     * Returns the defined method related to this declared method. The defined method
     * is always either defined by the same class or a superclass thereof.
     *
     * The behavior of this method is undefined if [[hasSingleDefinedMethod]] returns false.
     */
    def definedMethod: Method

    /**
     * If `true`, there are multiple methods that define this method and they can be iterated over
     * using [[foreachDefinedMethod]].
     */
    def hasMultipleDefinedMethods: Boolean

    /**
     * The definition of this method; defined iff [[hasMultipleDefinedMethods]] returns `true`.
     */
    def asMultipleDefinedMethods: MultipleDefinedMethods

    /**
     * Returns the defined method related to this declared method. The defined method
     * is always either defined by the same class or a superclass thereof.
     *
     * The behavior of this method is undefined if [[hasMultipleDefinedMethods]] returns false.
     */
    def definedMethods: ConstArray[Method]

    /**
     * Executes the given function for each method definition.
     *
     * The behavior of this method is undefined if neither [[hasSingleDefinedMethod]] nor
     * [[hasMultipleDefinedMethods]] returns true.
     */
    def foreachDefinedMethod[U](f: Method ⇒ U): Unit

}

/**
 * Represents a method belonging to the API of the specified class type, where the original
 * method definition is not available (in the context of the current analysis).
 * Note that one VirtualDeclaredMethod may represent more than one actual method, because a class
 * may have several package-private methods with the same signature.
 */
final case class VirtualDeclaredMethod private[br] (
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor
) extends DeclaredMethod {

    override def hasSingleDefinedMethod: Boolean = false
    override def definedMethod: Method = throw new UnsupportedOperationException();
    override def asDefinedMethod: DefinedMethod = throw new ClassCastException();

    override def hasMultipleDefinedMethods: Boolean = false
    override def definedMethods: ConstArray[Method] = throw new UnsupportedOperationException();
    override def asMultipleDefinedMethods: MultipleDefinedMethods = throw new ClassCastException();

    override def foreachDefinedMethod[U](f: Method ⇒ U): Unit = {
        throw new UnsupportedOperationException();
    }

    override def toString: String = {
        s"VirtualDeclaredMethod(${declaringClassType.toJava},$name,$descriptor)"
    }
}

/**
 * Represents a declared method; i.e., a method which belongs to the (public and private) API of a
 * class along with a reference to the original declaration.
 */
final case class DefinedMethod private[br] (
        declaringClassType: ReferenceType,
        definedMethod:      Method
) extends DeclaredMethod {

    override def name: String = definedMethod.name
    override def descriptor: MethodDescriptor = definedMethod.descriptor

    override def hasSingleDefinedMethod: Boolean = true
    override def asDefinedMethod: DefinedMethod = this

    override def hasMultipleDefinedMethods: Boolean = false
    override def definedMethods: ConstArray[Method] = throw new UnsupportedOperationException();
    override def asMultipleDefinedMethods: MultipleDefinedMethods = throw new ClassCastException();

    override def foreachDefinedMethod[U](f: Method ⇒ U): Unit = f(definedMethod)

    override def toString: String = {
        s"DefinedMethod(declaringClassType=${declaringClassType.toJava},definedMethod=${definedMethod.toJava})"
    }
}

final case class MultipleDefinedMethods private[br] (
        declaringClassType: ReferenceType,
        definedMethods:     ConstArray[Method]
) extends DeclaredMethod {

    override def name: String = definedMethods.head.name
    override def descriptor: MethodDescriptor = definedMethods.head.descriptor

    override def hasSingleDefinedMethod: Boolean = false
    override def asDefinedMethod: DefinedMethod = throw new ClassCastException();
    override def definedMethod: Method = throw new UnsupportedOperationException();

    override def hasMultipleDefinedMethods: Boolean = true
    override def asMultipleDefinedMethods: MultipleDefinedMethods = this

    override def foreachDefinedMethod[U](f: Method ⇒ U): Unit = definedMethods.foreach(f)

    override def toString: String = {
        s"DefinedMethod(${declaringClassType.toJava},definedMethods=${definedMethods.map(_.toJava).mkString("{", ", ", "}")})"
    }
}
