/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
 * Tests instantiation of `InvocationInstructions` using the convenience factory methods.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class FactoryMethodsOfInvocationInstructionTest extends FlatSpec {

    behavior of "factory methods of InvocationInstructions"

    val declaringClass = "my/invoke/Class"
    val methodName = "myMythod"
    val methodDescriptor = "()V"

    "INVOKEINTERFACE's factory method" should "return an INVOKEINTERFACE instruction" in {
        val invoke = INVOKEINTERFACE(declaringClass, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKEINTERFACE")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKEVIRTUAL's factory method" should "return an INVOKEVIRTUAL instruction" in {
        val invoke = INVOKEVIRTUAL(declaringClass, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKEVIRTUAL")
        assert(invoke.declaringClass.asObjectType.fqn == declaringClass)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESPECIAL's factory method" should "return an INVOKESPECIAL instruction" in {
        val invoke = INVOKESPECIAL(declaringClass, false, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKESPECIAL")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(!invoke.isInterface)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

    "INVOKESTATIC's factory method" should "return an INVOKESTATIC instruction" in {
        val invoke = INVOKESTATIC(declaringClass, false, methodName, methodDescriptor)

        assert(invoke.getClass.getName == "org.opalj.br.instructions.INVOKESTATIC")
        assert(invoke.declaringClass.fqn == declaringClass)
        assert(!invoke.isInterface)
        assert(invoke.name == methodName)
        assert(invoke.methodDescriptor.toJVMDescriptor == methodDescriptor)
    }

}
