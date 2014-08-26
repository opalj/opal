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
package br

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution

/**
 * Tests that Array types are represented as specified.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ArrayTypeTest extends FunSuite with ParallelTestExecution {

    test("ArrayType (Array of References) Field Descriptor") {
        val fieldType = FieldType("[Ljava/lang/Object;")
        val ArrayType(componentType) = fieldType
        assert(componentType === FieldType("Ljava/lang/Object;"))
    }

    test("ArrayType (Array of Primitives) Field Descriptor") {
        val fieldType = FieldType("[J")
        val ArrayType(componentType) = fieldType
        assert(componentType === LongType)
    }

    test("ArrayType (Array of Array of Primitives) Field Descriptor") {
        val fieldType = FieldType("[[S")
        fieldType match {
            case ArrayType(ArrayType(ShortType)) ⇒ /*OK*/
        }
    }

    test("toJavaClass") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at1Class = at1.toJavaClass
        assert(at1Class == classOf[Array[Object]])

        val at2 = ArrayType(ObjectType("java/util/List"))
        assert(at2.toJavaClass == classOf[Array[java.util.List[_]]])

        val at3 = ArrayType(IntegerType)
        assert(at3.toJavaClass == classOf[Array[Int]])
    }

    test("Equality") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at2 = FieldType("[Ljava/lang/Object;")
        val at3: ArrayType = FieldType("[[Ljava/lang/Object;").asInstanceOf[ArrayType]

        assert(at1 == at2)
        assert(at3.componentType == at2)
    }

    test("Reference equality") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at2 = FieldType("[Ljava/lang/Object;")
        val at3 = FieldType("[[Ljava/lang/Object;").asInstanceOf[ArrayType]
        val at4 = FieldType("[Ljava/lang/String;").asInstanceOf[ArrayType]

        assert(at1 eq at2)
        assert(at3.componentType eq at2)
        assert(at4.componentType ne at2)
    }

    test("Pattern matching") {
        val at1: FieldType = FieldType("[[[Ljava/lang/Object;")

        at1 match {
            case ArrayType(ArrayType(ArrayType(ObjectType(className)))) ⇒
                assert(className === "java/lang/Object")
        }
    }
}
