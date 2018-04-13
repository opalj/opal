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
     * a super class.
     */
    def declaringClassType: ReferenceType

    def name: String

    def descriptor: MethodDescriptor

    def toJava: String = s"${declaringClassType.toJava}{ ${descriptor.toJava(name)} }"

    /**
     * If `true` the method which actually defines this method (which may still be abstract!),
     * is known and is available using [[asDefinedMethod]].
     */
    def hasDefinition: Boolean

    /** The definition of this method; defined iff [[hasDefinition]] returns `true`. */
    def asDefinedMethod: DefinedMethod

    /**
     * Returns the defined method related to this declared method. The defined method
     * is always either defined by the same class or a superclass thereof.
     *
     * The behavior of this method is undefined if [[hasDefinition]] return false.
     */
    def methodDefinition: Method

    override def hashCode: Int = {
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + descriptor.hashCode()
    }
}

/**
 * Represents a method belonging to the API of the specified class type, where the original
 * method definition is not available (in the context of the current analysis.)
 */
final case class VirtualDeclaredMethod(
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor
) extends DeclaredMethod {

    override def hasDefinition: Boolean = false
    override def methodDefinition: Method = throw new UnsupportedOperationException("not available")
    override def asDefinedMethod: DefinedMethod = throw new ClassCastException()

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualDeclaredMethod ⇒
                (this.declaringClassType eq that.declaringClassType) &&
                    this.name == that.name &&
                    this.descriptor == that.descriptor
            case _ ⇒
                false
        }
    }

    override def toString: String = {
        s"VirtualDeclaredMethod($declaringClassType,$name,$descriptor)"
    }
}

/**
 * Represents a declared method; i.e., a method which belongs to the (public and private) API of a
 * class along with a reference to the original declaration.
 */
final case class DefinedMethod(
        declaringClassType: ReferenceType,
        definedMethod:      Method
) extends DeclaredMethod {

    override def name: String = definedMethod.name
    override def descriptor: MethodDescriptor = definedMethod.descriptor

    override def hasDefinition: Boolean = true
    override def asDefinedMethod: DefinedMethod = this
    override def methodDefinition: Method = definedMethod

    override def hashCode: Int = (definedMethod.hashCode() * 41) + super.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: DefinedMethod ⇒
                (this.definedMethod eq that.definedMethod) &&
                    (this.declaringClassType eq that.declaringClassType)
            case _ ⇒
                false
        }
    }

    override def toString: String = {
        s"DefinedMethod($declaringClassType,$name,$descriptor,${definedMethod.toJava})"
    }
}
