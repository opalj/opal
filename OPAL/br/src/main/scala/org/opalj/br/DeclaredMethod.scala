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
 * Represents a method of a virtual class.
 *
 * @author Michael Eichberg
 */
sealed abstract class DeclaredMethod {

    val declaringClassType: ReferenceType
    val name: String
    val descriptor: MethodDescriptor

    def toJava: String = declaringClassType.toJava+"{ "+descriptor.toJava(name)+"; }"

    override def hashCode: Int = {
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + descriptor.hashCode()
    }
}

final case class VirtualDeclaredMethod(
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor
) extends DeclaredMethod {

    override def toJava: String = declaringClassType.toJava+"{ "+descriptor.toJava(name)+" }"

    override def equals(other: Any): Boolean = other match {
        case that: VirtualDeclaredMethod ⇒
            (declaringClassType eq that.declaringClassType) && name == that.name && descriptor == that.descriptor
        case _ ⇒ false
    }

    override def toString: String = {
        s"VirtualDeclaredMethod($declaringClassType,$name,$descriptor)"
    }
}

final case class DefinedMethod(
        declaringClassType: ReferenceType,
        name:               String,
        descriptor:         MethodDescriptor,
        target:             Method
) extends DeclaredMethod {

    override def toJava: String = declaringClassType.toJava+"{ "+descriptor.toJava(name)+" }"

    override def hashCode: Int = (target.hashCode() * 41) + super.hashCode

    override def equals(other: Any): Boolean = other match {
        case that: DefinedMethod ⇒
            (this.target eq that.target) && (declaringClassType eq that.declaringClassType) && name == that.name && descriptor == that.descriptor
        case _ ⇒ false
    }

    override def toString: String = {
        s"DefinedMethod($declaringClassType,$name,$descriptor,$target)"
    }
}

