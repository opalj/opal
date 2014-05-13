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
package org.opalj
package de

import br._

/**
 * Enumeration of all kinds of dependencies that are extracted by the
 * [[DependencyExtractor]].
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
object DependencyType extends Enumeration(0 /* <= value of first enumeration value*/ ) {

    val EXTENDS = Value("type declaration EXTENDS class type")
    val IMPLEMENTS = Value("type declaration IMPLEMENTS interface type")

    val OUTER_CLASS = Value("class type declaration IS OUTER CLASS of method or type")
    val INNER_CLASS = Value("class type declaration IS INNER CLASS of method or type")

    val INSTANCE_MEMBER = Value("field or method IS INSTANCE MEMBER of class type")
    val CLASS_MEMBER = Value("field/method IS CLASS MEMBER of class/interface/annotation/enum")
    val ENCLOSED = Value("class type declaration IS ENCLOSED by method or type")

    // field definition related dependency types
    val FIELD_TYPE = Value("the FIELD has TYPE")
    val CONSTANT_VALUE = Value("the field is initialized with a CONSTANT VALUE with TYPE")

    // method definition related dependency types
    val PARAMETER_TYPE = Value("the method defines a PARAMETER with TYPE")
    val RETURN_TYPE = Value("the method's RETURN TYPE")
    val THROWN_EXCEPTION = Value("the method may THROW an exception of type")

    // code related dependency types
    val CATCHES = Value("catches")

    val LOCAL_VARIABLE_TYPE = Value("the method has a LOCAL VARIABLE with TYPE")

    val TYPECAST = Value("performs a refrence based TYPE CAST")
    val TYPECHECK = Value("performs a TYPE CHECK using \"instanceOf\"")

    val CREATES_ARRAY = Value("CREATES a new ARRAY of type")
    val CREATES = Value("CREATES a new instance of class")

    val READS_FIELD = Value("the method READS the value stored in the FIELD")
    val WRITES_FIELD = Value("the method WRITES the value stored in the FIELD")
    val DECLARING_CLASS_OF_ACCESSED_FIELD = Value("the method ACCESSES a FIELD that is DECLARED by CLASS")
    val TYPE_OF_ACCESSED_FIELD = Value("the method ACCESSES a FIELD with TYPE")

    val CALLS_METHOD = Value("the method CALLS the METHOD")
    val DECLARING_CLASS_OF_CALLED_METHOD = Value("the CALLED METHOD is DECLARED by TYPE")
    val PARAMETER_TYPE_OF_CALLED_METHOD = Value("the method CALLS a METHOD that has a PARAMETER with TYPE")
    val RETURN_TYPE_OF_CALLED_METHOD = Value("the method CALLS a METHOD that RETURNS a value with TYPE")

    // annotation related dependency types
    val ANNOTATED_WITH = Value("the class, field, method, type parameter, local variable, ... is ANNOTATED WITH")
    val PARAMETER_ANNOTATED_WITH = Value("the method's parameter is ANNOTATED WITH")

    // element value related dependency type
    val ANNOTATION_DEFAULT_VALUE_TYPE = Value("the TYPE of the ANNOTATIONS DEFAULT VALUE")
    val ANNOTATION_ELEMENT_TYPE = Value("the TYPE of the ANNOTATION's ELEMENT value")
    val USES_ENUM_VALUE = Value("the annotation element's value is the ENUM VALUE")

    // signature/type parameter related dependency types
    val TYPE_IN_TYPE_PARAMETERS = Value("the TYPE is used in the declaration of a TYPE PARAMETER (signature)")

    def bitMask(v: Value): Long = 1l << v.id

    def toSet(set: DependencyTypesBitSet): scala.collection.Set[DependencyType] = {
        val max = maxId
        var i = 0
        val dependencies = new scala.collection.mutable.HashSet[DependencyType]
        while (i <= max) {
            if (((set >> i) & 1) == 1)
                dependencies += DependencyType(i)
            i += 1
        }
        dependencies
    }
}
