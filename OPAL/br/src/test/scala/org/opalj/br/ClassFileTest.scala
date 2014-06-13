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
import org.scalatest.Matchers

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ClassFileTest extends FunSuite with Matchers with ParallelTestExecution {

    import reader.Java8Framework.ClassFile

    val codeJARFile = TestSupport.locateTestResources("classfiles/Code.jar", "bi")
    val immutableList = ClassFile(codeJARFile, "code/ImmutableList.class")
    val boundedBuffer = ClassFile(codeJARFile, "code/BoundedBuffer.class")
    val quicksort = ClassFile(codeJARFile, "code/Quicksort.class")

    test("test that it can find the first constructor") {
        assert(
            immutableList.findMethod(
                "<init>",
                MethodDescriptor(ObjectType.Object, VoidType)
            ).isDefined
        )
    }

    test("test that it can find the second constructor") {
        assert(
            immutableList.findMethod(
                "<init>",
                MethodDescriptor(
                    IndexedSeq(ObjectType.Object, ObjectType("code/ImmutableList")),
                    VoidType)
            ).isDefined
        )

    }

    test("test that it can find all other methods") {
        assert(
            immutableList.findMethod(
                "getNext",
                MethodDescriptor(IndexedSeq(), ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "prepend",
                MethodDescriptor(ObjectType.Object, ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "getIterator",
                MethodDescriptor(IndexedSeq(), ObjectType("java/util/Iterator"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "get",
                MethodDescriptor(IndexedSeq(), ObjectType.Object)
            ).isDefined
        )
    }

    test("that findField on a class without fields does not fail") {
        quicksort.fields should be(empty)
        quicksort.findField("DoesNotExist") should be(None)
    }

    test("that findField finds all fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("buffer") should be('defined)
        boundedBuffer.findField("first") should be('defined)
        boundedBuffer.findField("last") should be('defined)
        boundedBuffer.findField("size") should be('defined)
        boundedBuffer.findField("numberInBuffer") should be('defined)
    }
    
    test("that findField does not find non-existing fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("BUFFER") should be(None)
        boundedBuffer.findField("firsT") should be(None)
        boundedBuffer.findField("lAst") should be(None)
        boundedBuffer.findField("Size") should be(None)
        boundedBuffer.findField("AnumberInBuffers") should be(None)
    }

}
