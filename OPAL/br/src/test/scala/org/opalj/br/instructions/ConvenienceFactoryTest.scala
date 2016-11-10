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
package instructions

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

/**
 * Tests instantiation of InvocationInstructions with the convenience Constructors with only
 * Strings as parameter for the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class ConvenienceConstructorInvocationInstructionTest extends FlatSpec {
    behavior of "InvocationInstructions convenience factories"

    val declaringClass = "my/test/Class"
    val methodName = "myMythod"
    val methodDescriptor = "()V"

    "INVOKEINTERFACE instantiation" should "return an INVOKEINTERFACE instruction" in {
        val test = INVOKEINTERFACE(declaringClass, methodName, methodDescriptor)

        assert(test.getClass.getName == "org.opalj.br.instructions.INVOKEINTERFACE")
        assert(test.declaringClass.fqn == declaringClass)
        assert(test.name == methodName)
        assert(test.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKEVIRTUAL instantiation" should "return an INVOKEVIRTUAL instruction" in {
        val test = INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)

        assert(test.getClass.getName == "org.opalj.br.instructions.INVOKEVIRTUAL")
        assert(test.declaringClass.asInstanceOf[ObjectType].fqn == declaringClass)
        assert(test.name == methodName)
        assert(test.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESPECIAL instantiation" should "return an INVOKESPECIAL instruction" in {
        val test = INVOKESPECIAL(declaringClass, false, methodName, methodDescriptor)

        assert(test.getClass.getName == "org.opalj.br.instructions.INVOKESPECIAL")
        assert(test.declaringClass.fqn == declaringClass)
        assert(!test.isInterface)
        assert(test.name == methodName)
        assert(test.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESTATIC instantiation" should "return an INVOKESTATIC instruction" in {
        val test = INVOKESTATIC(declaringClass, false, methodName, methodDescriptor)

        assert(test.getClass.getName == "org.opalj.br.instructions.INVOKESTATIC")
        assert(test.declaringClass.fqn == declaringClass)
        assert(!test.isInterface)
        assert(test.name == methodName)
        assert(test.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

}
