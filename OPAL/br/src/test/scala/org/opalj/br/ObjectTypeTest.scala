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
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ObjectTypeTest extends FunSuite with ParallelTestExecution {

    test("ObjectType Field Descriptor") {
        val fieldType = FieldType("Ljava/lang/Object;")
        val ObjectType(className) = fieldType
        assert(className === "java/lang/Object")
    }

    test("toJavaClass") {
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")
        val ot4 = ObjectType("java/util/List")

        assert(ot2.toJavaClass == classOf[Object])
        assert(ot3.toJavaClass == classOf[String])
        assert(ot4.toJavaClass == classOf[java.util.List[_]])
    }

    test("Structural Equality") {
        val ot1 = ObjectType("java/lang/Object")
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")

        assert(ot1 == ot2)
        assert(ot1 != ot3)
    }

    test("Reference equality") {
        val ot1 = ObjectType("java/lang/Object")
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")

        assert(ot1 eq ot2)
        assert(ot1 ne ot3)
    }

    test("Pattern matching") {
        val ot1: FieldType = ObjectType("java/lang/Object")
        ot1 match { case ObjectType(c) ⇒ assert(c === "java/lang/Object") }
    }

    test("onPrimitiveWrapperMatch") {

        val matcher = ObjectType.primitiveWrapperMatcher[Int, (Int, Int)](
            (id) ⇒ (id, BooleanType.WrapperType.id),
            (id) ⇒ (id, ByteType.WrapperType.id),
            (id) ⇒ (id, CharType.WrapperType.id),
            (id) ⇒ (id, ShortType.WrapperType.id),
            (id) ⇒ (id, IntegerType.WrapperType.id),
            (id) ⇒ (id, LongType.WrapperType.id),
            (id) ⇒ (id, FloatType.WrapperType.id),
            (id) ⇒ (id, DoubleType.WrapperType.id),
            (id) ⇒ (-1, id)
        )
        assert(matcher(BooleanType.WrapperType, 1) == (1, BooleanType.WrapperType.id))
        assert(matcher(ByteType.WrapperType, 2) == (2, ByteType.WrapperType.id))
        assert(matcher(CharType.WrapperType, 3) == (3, CharType.WrapperType.id))
        assert(matcher(ShortType.WrapperType, 4) == (4, ShortType.WrapperType.id))
        assert(matcher(IntegerType.WrapperType, 5) == (5, IntegerType.WrapperType.id))
        assert(matcher(LongType.WrapperType, 6) == (6, LongType.WrapperType.id))
        assert(matcher(FloatType.WrapperType, 7) == (7, FloatType.WrapperType.id))
        assert(matcher(DoubleType.WrapperType, 8) == (8, DoubleType.WrapperType.id))
        assert(matcher(ObjectType.String, ObjectType.String.id) == (-1, ObjectType.String.id))

        assert(matcher(BooleanType.WrapperType, 10) == (10, BooleanType.WrapperType.id))
        assert(matcher(ByteType.WrapperType, 20) == (20, ByteType.WrapperType.id))
        assert(matcher(CharType.WrapperType, 30) == (30, CharType.WrapperType.id))
        assert(matcher(ShortType.WrapperType, 40) == (40, ShortType.WrapperType.id))
        assert(matcher(IntegerType.WrapperType, 50) == (50, IntegerType.WrapperType.id))
        assert(matcher(LongType.WrapperType, 60) == (60, LongType.WrapperType.id))
        assert(matcher(FloatType.WrapperType, 70) == (70, FloatType.WrapperType.id))
        assert(matcher(DoubleType.WrapperType, 80) == (80, DoubleType.WrapperType.id))
        assert(matcher(ObjectType.String, ObjectType.String.id) == (-1, ObjectType.String.id))
    }

}
