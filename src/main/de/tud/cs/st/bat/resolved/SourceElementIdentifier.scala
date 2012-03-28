/* License (BSD Style License):
 *  Copyright (c) 2009, 2011
 *  Software Technology Group
 *  Department of Computer Science
 *  Technische Universität Darmstadt
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.resolved

/**
 * Uniquely identifies a specific source element (Type, Field or Method declaration).
 *
 * @author Michael Eichberg
 */
trait SourceElementIdentifier {

    /**
     * Returns a compact, human readable representation of this source element's id.
     * This representation is not guaranteed to return a unique representation.
     */
    def toHRR: String

    /**
     * Returns the name of the package in which this source element is defined. If
     * this source element (e.g., a primitive type) does not belong to a specific
     * pacakge None is returned. In case of the default pacakge, the empty string
     * is returned.
     */
    def declaringPackage: Option[String]
}

case class TypeIdentifier(t: Type) extends SourceElementIdentifier {

    def toHRR = t.toJava

    def declaringPackage =
        t match {
            case o: ObjectType ⇒ Some(o.packageName);
            case _             ⇒ None
        }
}

case class MethodIdentifier(declaringReferenceType: ReferenceType,
                            methodName: String,
                            methodDescriptor: MethodDescriptor)
        extends SourceElementIdentifier {

    def toHRR = declaringReferenceType.toJava+"."+methodName+""+(methodDescriptor.toUMLNotation)

    def declaringPackage =
        declaringReferenceType match {
            case o: ObjectType ⇒ Some(o.packageName);
            case a: ArrayType  ⇒ Some("java/lang"); // required to handle "clone" calls on arrays
            case _             ⇒ None
        }
}

case class FieldIdentifier(declaringObjectType: ObjectType,
                           fieldName: String)
        extends SourceElementIdentifier {
    def toHRR = declaringObjectType.toJava+"."+fieldName

    def declaringPackage = Some(declaringObjectType.packageName)
}
