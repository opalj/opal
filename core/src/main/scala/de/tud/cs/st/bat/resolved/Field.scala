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
package de.tud.cs.st.bat
package resolved

/**
 * Represents a single field declaration/definition.
 *
 * @param accessFlags This field's access flags. To analyze the access flags
 *  bit vector use [[de.tud.cs.st.bat.AccessFlag]] or [[de.tud.cs.st.bat.AccessFlagsIterator]].
 * @param name The name of this field. Note, that this name may be no valid
 *  Java programming language identifier.
 * @param fieldType The (erased) type of this field.
 * @param attributes The defined attributes. The JVM 7 specification defines the following
 * 	attributes for fields: [[de.tud.cs.st.bat.resolved.ConstantValue]], [[de.tud.cs.st.bat.resolved.Synthetic]], [[de.tud.cs.st.bat.resolved.Signature]],
 * 	[[de.tud.cs.st.bat.resolved.Deprecated]], [[de.tud.cs.st.bat.resolved.RuntimeVisibleAnnotationTable]] and [[de.tud.cs.st.bat.resolved.RuntimeInvisibleAnnotationTable]].
 *
 * @author Michael Eichberg
 */
case class Field(accessFlags: Int,
                 name: String,
                 fieldType: FieldType,
                 attributes: Attributes)
        extends ClassMember {

    override def isField = true

    override def asField = this

    def isTransient: Boolean = ACC_TRANSIENT element_of accessFlags

    def isVolatile: Boolean = ACC_VOLATILE element_of accessFlags

    /**
     * Returns this field's type signature.
     */
    def fieldTypeSignature: Option[FieldTypeSignature] =
        attributes collectFirst { case s: FieldTypeSignature ⇒ s }

    /**
     * Returns this field's constant value.
     */
    def constantValue: Option[ConstantValue[_]] =
        attributes collectFirst { case cv: ConstantValue[_] ⇒ cv }

}