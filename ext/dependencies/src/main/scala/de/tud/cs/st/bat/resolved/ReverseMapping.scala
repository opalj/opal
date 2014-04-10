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
 * Associates each unique ID with its underlying source element.
 *
 * @author Michael Eichberg
 */
@deprecated(message="All source elements now always have an idea and the project provides a reverse mapping",since="2014")
trait ReverseMapping extends CategorizedSourceElementIDs {

    import scala.collection.mutable.ArrayBuffer

    private[this] val typeIDs =
        new ArrayBuffer[Type](50000)

    private[this] val fieldIDs =
        new ArrayBuffer[(ObjectType, String)](100000)

    private[this] val methodIDs =
        new ArrayBuffer[(ReferenceType, String, MethodDescriptor)](250000)

    abstract override def sourceElementID(t: Type): Int = {
        val id = super.sourceElementID(t)
        val index = id - LOWEST_TYPE_ID
        if (index >= typeIDs.size) // the type may already have an associated ID
            typeIDs.insert(index, t)
        id
    }

    abstract override def sourceElementID(
        declaringType: ObjectType,
        fieldName: String): Int = {

        val id = super.sourceElementID(declaringType, fieldName)
        val index = id - LOWEST_FIELD_ID
        if (index >= fieldIDs.size)
            fieldIDs.insert(index, (declaringType, fieldName))
        id
    }

    abstract override def sourceElementID(
        declaringType: ReferenceType,
        methodName: String,
        methodDescriptor: MethodDescriptor): Int = {

        val id = super.sourceElementID(declaringType, methodName, methodDescriptor)
        val index = id - LOWEST_METHOD_ID
        if (index >= methodIDs.size)
            methodIDs.insert(index, (declaringType, methodName, methodDescriptor))
        id
    }

    /**
     * Returns a human readable representation of the source element identified by the
     * given id.
     *
     * @param id A valid identifier; i.e., an id returned by one of the sourceElementID methods.
     *  If the id is not valid, the behavior is undefined.
     */
    def sourceElementIDtoString(id: Int): String = {
        if (id < LOWEST_FIELD_ID) {
            return typeIDs(id - LOWEST_TYPE_ID).toJava
        }
        // ID >= LOWEST_FIELD_ID
        if (id < LOWEST_METHOD_ID) {
            val (declaringType, fieldName) = fieldIDs(id - LOWEST_FIELD_ID)
            return declaringType.toJava+"."+fieldName
        }

        // ID >= LOWEST_METHOD_ID
        val (declaringType, methodName, methodDescriptor) = methodIDs(id - LOWEST_METHOD_ID)
        return declaringType.toJava+"."+methodName+""+(methodDescriptor.toUMLNotation)
    }

}
