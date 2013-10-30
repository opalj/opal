/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
 * Associates a source element (type/class, method or field declaration) with
 * a unique id.
 *
 * ==Adaptation==
 * If you want to be able to efficiently retrieve a source element when you have an id,
 * consider mixing in the [[de.tud.cs.st.bat.resolved.ReverseMapping]] trait.
 *
 * @author Michael Eichberg
 */
trait SourceElementIDs {

    /**
     * Returns the ID associated with the given type. This ID uniquely identifies this
     * type declaration. If the type does not yet has an associated id, a
     * new id is created and returned.
     *
     * @return The ID associated with the given type.
     */
    def sourceElementID(t: Type): Int

    /**
     * Returns the ID associated with the given field. This ID uniquely identifies
     * the specified field.  If the field does not yet has an associated id,
     * new id is created and returned.
     */
    def sourceElementID(
        declaringObjectType: ObjectType,
        fieldName: String): Int

    /**
     * Returns the ID associated with the given method. This ID uniquely identifies
     * the specified method. If the method does not yet has an associated id,
     * new id is created and returned.
     */
    def sourceElementID(
        declaringReferenceType: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor): Int

    final def sourceElementID(classFile: ClassFile): Int =
        sourceElementID(classFile.thisClass)

    final def sourceElementID(declaringClassFile: ClassFile, field: Field): Int =
        sourceElementID(declaringClassFile.thisClass, field.name)

    final def sourceElementID(declaringObjectType: ObjectType, field: Field): Int =
        sourceElementID(declaringObjectType, field.name)

    final def sourceElementID(declaringClassFile: ClassFile, method: Method): Int =
        sourceElementID(declaringClassFile.thisClass, method)

    final def sourceElementID(declaringReferenceType: ReferenceType, method: Method): Int = {
        val Method(_, methodName, methodDescriptor, _) = method
        sourceElementID(declaringReferenceType, methodName, methodDescriptor)
    }
}
