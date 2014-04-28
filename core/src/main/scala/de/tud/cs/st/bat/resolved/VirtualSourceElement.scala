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
package de.tud.cs.st
package bat
package resolved

/**
 * A `VirtualSourceElement` is the representation of some source element that is
 * always detached from the concrete source element that represents the implementation.
 *
 * @author Michael Eichberg
 */
sealed trait VirtualSourceElement extends SourceElement {

    override def attributes = Nil

    final override def isVirtual = true
}

/**
 * Represents a class for which we have found some references but have not analyzed
 * any class file or do not want to keep the reference to the underlying class file.
 *
 * @author Michael Eichberg
 */
final case class VirtualClass(
        thisType: ObjectType,
        fields: Set[VirtualField] = Set.empty,
        methods: Set[VirtualMethod] = Set.empty) extends VirtualSourceElement {

    override def isClass = true

    override def hashCode = thisType.id

    /**
     * Two objects of type `VirtualClass` are considered equal if they represent
     * the same type.
     */
    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualClass ⇒ this.thisType eq that.thisType
            case _                  ⇒ false
        }
    }
}

/**
 * @author Michael Eichberg
 */
sealed trait VirtualClassMember extends VirtualSourceElement

/**
 * Represents a field of a virtual class.
 *
 * @author Michael Eichberg
 */
final case class VirtualField(
        declaringClassType: ReferenceType,
        name: String,
        fieldType: FieldType) extends VirtualClassMember {

    override def isField = true

    override def hashCode =
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + fieldType.id

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualField ⇒
                (this.declaringClassType eq this.declaringClassType) &&
                    (this.fieldType eq that.fieldType) &&
                    this.name == that.name
            case _ ⇒ false
        }
    }
}

/**
 * Represents a method.
 *
 * @author Michael Eichberg
 */
final case class VirtualMethod(
        declaringClassType: ReferenceType,
        name: String,
        descriptor: MethodDescriptor) extends VirtualClassMember {

    override def isMethod = true

    override def hashCode =
        (((declaringClassType.id * 41) + name.hashCode()) * 41) + descriptor.hashCode()

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualMethod ⇒
                (this.declaringClassType eq this.declaringClassType) &&
                    (this.descriptor eq that.descriptor) &&
                    this.name == that.name
            case _ ⇒ false
        }
    }
}