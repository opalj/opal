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
 * Uniquely identifies a specific element that can (by definition) only exist
 * once in a project. For example, in the context of OPAL a type declaration
 * is unique or the combination of a type declaration and a field name or
 * the combination of a type declaration, method name and method descriptor.
 *
 * @author Michael Eichberg
 */
trait StructureIdentifier {

    /**
     * Returns a compact, human readable representation of this structure element.
     * This representation is not guaranteed to return a unique representation.
     * However, it should be precise enough to enable developers (with some
     * additional context information) to precisely identify the structure element.
     */
    def toHRR: String

    /**
     * Returns the name of the package in which this structure element is defined. If
     * this element (e.g., a primitive type) does not belong to a specific
     * package or the concept of a package name does not apply None is returned.
     * In case of the default package, the empty string is returned.
     */
    def declaringPackage: Option[String]
}

case class TypeIdentifier(t: Type) extends StructureIdentifier {

    def toHRR = t.toJava

    def declaringPackage =
        t match {
            case o: ObjectType ⇒ Some(o.packageName);
            case _             ⇒ None
        }
}

case class MethodIdentifier(
    declaringReferenceType: ReferenceType,
    methodName: String,
    methodDescriptor: MethodDescriptor)
        extends StructureIdentifier {

    def toHRR =
        declaringReferenceType.toJava+"."+methodName+""+(methodDescriptor.toUMLNotation)

    def declaringPackage =
        declaringReferenceType match {
            case o: ObjectType            ⇒ Some(o.packageName);
            case ArrayType(o: ObjectType) ⇒ Some(o.packageName);
            case a: ArrayType             ⇒ Some("java/lang"); // handles calls on Arrays of primitives
            case _                        ⇒ None
        }
}

case class FieldIdentifier(
    declaringObjectType: ObjectType,
    fieldName: String)
        extends StructureIdentifier {

    def toHRR = declaringObjectType.toJava+"."+fieldName

    def declaringPackage = Some(declaringObjectType.packageName)
}
