/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ba

import java.io.ByteArrayInputStream

import reflect.runtime.universe._
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import org.opalj.bc.Assembler
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the branchoffset calculation of LabeledBranchInstructions in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class JumpLabelsTest extends FlatSpec {

    val testClass = (PUBLIC CLASS "TestJump" EXTENDS "java/lang/Object")(
        //returns the given int
        //includes forward and backward jumps
        PUBLIC("returnInt", "(I)", "I")(
            CODE(
                GOTO('IsZero_?),
                'Else,
                ILOAD_1,
                IRETURN,
                'IsTwo_?,
                ILOAD_1,
                ICONST_2,
                IF_ICMPNE('Else),
                ICONST_2,
                IRETURN,
                'IsOne_?,
                ILOAD_1,
                ICONST_1,
                IF_ICMPNE('IsTwo_?),
                ICONST_1,
                IRETURN,
                'IsZero_?,
                ILOAD_1,
                IFNE('IsOne_?),
                ICONST_0,
                IRETURN
            )
        )
    )
    val assembledCF = Assembler(testClass.buildDAClassFile._1)

    val brClassFile = Java8Framework.ClassFile(() ⇒ new ByteArrayInputStream(assembledCF)).head

    "the method returnInt" should "execute as expected" in {
        val loader = new InMemoryClassLoader(
            Map("TestJump" → assembledCF),
            this.getClass.getClassLoader
        )

        val clazz = loader.loadClass("TestJump")
        val testJumpInstance = clazz.newInstance()

        val mirror = runtimeMirror(loader).reflect(testJumpInstance)
        val method = mirror.symbol.typeSignature.member(TermName("returnInt")).asMethod

        assert(mirror.reflectMethod(method)(0) == 0)
        assert(mirror.reflectMethod(method)(1) == 1)
        assert(mirror.reflectMethod(method)(2) == 2)
        assert(mirror.reflectMethod(method)(10) == 10)
    }

    "each BranchInstruction" should "have the correct branch offset" in {
        val instructions = brClassFile.methods.find(_.name == "returnInt").get.body.get.instructions
        assert(instructions(0).asInstanceOf[GOTO].branchoffset == 19)
        assert(instructions(7).asInstanceOf[IF_ICMPNE].branchoffset == -4)
        assert(instructions(14).asInstanceOf[IF_ICMPNE].branchoffset == -9)
        assert(instructions(20).asInstanceOf[IFNE].branchoffset == -8)
    }
}
