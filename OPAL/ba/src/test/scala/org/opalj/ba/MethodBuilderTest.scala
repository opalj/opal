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
package ba

import reflect.runtime.universe._
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.bc.Assembler
import org.opalj.bi._
import org.opalj.br.MethodDescriptor
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the properties of a method in a class build with the BytecodeAssembler DSL. The class is
 * build, assembled as a [[org.opalj.da.ClassFile]] and read again as a [[org.opalj.br.ClassFile]].
 * It is also loaded, instantiated and the methods are executed with the JVM.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class MethodBuilderTest extends FlatSpec {
    behavior of "Method Attributes"

    val simpleMethodClass = (PUBLIC CLASS "SimpleMethodClass" EXTENDS "java/lang/Object")(
        FINAL + SYNTHETIC + PUBLIC("testMethod", "(Ljava/lang/String;)", "Ljava/lang/String;")(
            CODE(
                ACONST_NULL,
                ARETURN
            )
        )
    )

    val assembledCF = Assembler(simpleMethodClass.buildDAClassFile._1)

    "the generated method 'SimpleMethodClass.testMethod'" should "execute correctly" in {
        val loader = new InMemoryClassLoader(
            Map("SimpleMethodClass" → assembledCF),
            this.getClass.getClassLoader
        )

        val simpleMethodClazz = loader.loadClass("SimpleMethodClass")
        val simpleMethodInstance = simpleMethodClazz.newInstance()
        val mirror = runtimeMirror(loader).reflect(simpleMethodInstance)
        val method = mirror.symbol.typeSignature.member(TermName("testMethod")).asMethod

        assert(mirror.reflectMethod(method)("test") == null)
    }

    val brClassFile = Java8Framework.ClassFile(() ⇒ new java.io.ByteArrayInputStream(assembledCF)).head

    val testMethod = brClassFile.methods.find { m ⇒
        m.name == "testMethod" &&
            m.descriptor == MethodDescriptor("(Ljava/lang/String;)Ljava/lang/String;")
    }

    it should "have the correct signature: (Ljava/lang/String;)Ljava/lang/String;" in {
        assert(testMethod.isDefined)
    }

    it should "be public final synthetic" in {
        assert(
            testMethod.get.accessFlags == (ACC_PUBLIC.mask | ACC_FINAL.mask | ACC_SYNTHETIC.mask)
        )
    }

    "maxLocals of method SimpleMethodClass.testMethod" should "be set automatically to: 2" in {
        assert(testMethod.get.body.get.maxLocals == 2)
    }

    "maxStack of method SimpleMethodClass.testMethod" should "be set automatically to: 1" in {
        assert(testMethod.get.body.get.maxStack == 1)
    }

    val attributeMethodClass = (PUBLIC CLASS "AttributeMethodClass" EXTENDS "java/lang/Object")(
        PUBLIC("<init>", "()", "V")(
            CODE(
                ALOAD_0,
                INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                RETURN
            ) MAXSTACK 2 MAXLOCALS 3
        )
    )

    val assembledAttributeCF = Assembler(attributeMethodClass.buildDAClassFile._1)
    val attributeBrClassFile = Java8Framework.ClassFile(
        () ⇒ new java.io.ByteArrayInputStream(assembledAttributeCF)
    ).head

    val attributeTestMethod = attributeBrClassFile.methods.find { m ⇒
        m.name == "<init>" && m.descriptor == MethodDescriptor("()V")
    }.get

    val loader = new InMemoryClassLoader(
        Map("AttributeMethodClass" → assembledAttributeCF),
        this.getClass.getClassLoader
    )

    "the generated method 'AttributeMethodClass.<init>'" should "have 'maxStack' set to: 2" in {
        assert(attributeTestMethod.body.get.maxStack == 2)
    }

    it should "have 'maxLocals' set to: 3" in {
        assert(attributeTestMethod.body.get.maxLocals == 3)
    }

}
