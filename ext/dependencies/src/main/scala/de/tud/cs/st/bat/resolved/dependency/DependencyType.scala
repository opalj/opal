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
package dependency

/**
 * Enumeration of all kinds of dependencies that are extracted by the
 * [[DependencyExtractor]].
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
object DependencyType extends Enumeration {

    val EXTENDS = Value("type declaration EXTENDS class type")
    val IMPLEMENTS = Value("type declaration IMPLEMENTS interface type")

    val IS_INSTANCE_MEMBER = Value("field or method IS INSTANCE MEMBER of class type")

    val IS_CLASS_MEMBER = Value("field/method IS CLASS MEMBER of class/interface/annotation/enum")

    val IS_ENCLOSED = Value("class type declaration IS ENCLOSED by method or type")

    val IS_OUTER_CLASS = Value("class type declaration IS OUTER CLASS of method or type")
    val IS_INNER_CLASS = Value("class type declaration IS INNER CLASS of method or type")

    // field definition related dependency types
    val IS_OF_TYPE = Value("is of type")
    val USES_CONSTANT_VALUE_OF_TYPE = Value("uses constant value of type")

    // method definition related dependency types
    val RETURNS = Value("returns")
    val HAS_PARAMETER_OF_TYPE = Value("has parameter of type")
    val THROWS = Value("throws")
    val CATCHES = Value("catches")

    // code related dependency types
    val HAS_LOCAL_VARIABLE_OF_TYPE = Value("has local variable of type")
    val CREATES_ARRAY_OF_TYPE = Value("creates array of type")
    val CASTS_INTO = Value("casts into")
    val CHECKS_INSTANCEOF = Value("checks instanceOf")
    val CREATES = Value("creates") // TODO CREATES == newObject?
    val USES_FIELD_DECLARING_TYPE = Value("uses field declaring type")
    val READS_FIELD = Value("reads field")
    val WRITES_FIELD = Value("writes field")
    val USES_FIELD_READ_TYPE = Value("uses field read type")
    val USES_FIELD_WRITE_TYPE = Value("uses field write type")
    val USES_PARAMETER_TYPE = Value("uses parameter type")
    val USES_RETURN_TYPE = Value("uses return type")
    val USES_METHOD_DECLARING_TYPE = Value("uses method declaring type")
    val CALLS_METHOD = Value("calls method")
    val CALLS_INTERFACE_METHOD = Value("calls interface method")

    // annotation related dependency types
    val ANNOTATED_WITH = Value("annotated with")
    val PARAMETER_ANNOTATED_WITH = Value("parameter annotated with")

    // element value related dependency type
    val USES_DEFAULT_BASE_TYPE = Value("uses default base type")
    val USES_DEFAULT_CLASS_VALUE_TYPE = Value("uses default class value type")
    val USES_DEFAULT_ENUM_VALUE_TYPE = Value("uses default enum value type")
    val USES_ENUM_VALUE = Value("uses enum value")
    val USES_DEFAULT_ANNOTATION_VALUE_TYPE = Value("uses default annotation value type")

    // signature/type parameter related dependency types
    val USES_TYPE_IN_TYPE_PARAMETERS = Value("uses type in type parameters")

}
