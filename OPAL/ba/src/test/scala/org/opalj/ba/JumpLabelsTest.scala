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

import scala.language.postfixOps

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import java.io.ByteArrayInputStream

import scala.reflect.runtime.universe._

import org.opalj.bc.Assembler
import org.opalj.br.Method
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework.{ClassFile ⇒ ClassFileReader}
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the branchoffset calculation of LabeledBranchInstructions in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class JumpLabelsTest extends FlatSpec {

    val methodTemplate =
        METHOD(PUBLIC, "returnInt", "(I)I", CODE(
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
        ))

    val (daJava5ClassFile, _) =
        CLASS(
            version = bi.Java5Version,
            accessModifiers = PUBLIC SUPER,
            thisType = "TestJumpJava5",
            methods = METHODS(methodTemplate)
        ).toDA()
    val rawJava5ClassFile = Assembler(daJava5ClassFile)
    val brJava5ClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(rawJava5ClassFile)).head

    // We basically test that we compute the (correct) stack map table attribute
    val (daJava8ClassFile, _) =
        CLASS(
            version = bi.Java8Version,
            accessModifiers = PUBLIC SUPER,
            thisType = "TestJumpJava8",
            methods = METHODS(methodTemplate)
        ).toDA()
    val rawJava8ClassFile = Assembler(daJava8ClassFile)
    val brJava8ClassFile = ClassFileReader(() ⇒ new ByteArrayInputStream(rawJava8ClassFile)).head

    "the method returnInt" should "execute as expected" in {
        val classes = Map("TestJumpJava5" → rawJava5ClassFile, "TestJumpJava8" → rawJava8ClassFile)
        val loader = new InMemoryClassLoader(classes, this.getClass.getClassLoader)
        def testClass(clazz: Class[_]): Unit = {
            val testJumpInstance = clazz.getDeclaredConstructor().newInstance()

            val mirror = runtimeMirror(loader).reflect(testJumpInstance)
            val method = mirror.symbol.typeSignature.member(TermName("returnInt")).asMethod

            assert(mirror.reflectMethod(method)(0) == 0)
            assert(mirror.reflectMethod(method)(1) == 1)
            assert(mirror.reflectMethod(method)(2) == 2)
            assert(mirror.reflectMethod(method)(10) == 10)
        }

        testClass(loader.loadClass("TestJumpJava5"))
        testClass(loader.loadClass("TestJumpJava8"))
    }

    "each BranchInstruction" should "have the correct branch offset" in {
        def testMethods(methods: Seq[Method]): Unit = {
            val instructions = methods.find(_.name == "returnInt").get.body.get.instructions
            assert(instructions(0).asInstanceOf[GOTO].branchoffset == 19)
            assert(instructions(7).asInstanceOf[IF_ICMPNE].branchoffset == -4)
            assert(instructions(14).asInstanceOf[IF_ICMPNE].branchoffset == -9)
            assert(instructions(20).asInstanceOf[IFNE].branchoffset == -8)
        }

        testMethods(brJava5ClassFile.methods)
        testMethods(brJava8ClassFile.methods)
    }
}
