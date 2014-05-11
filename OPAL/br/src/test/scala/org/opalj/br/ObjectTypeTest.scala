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

}
