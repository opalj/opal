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
    behavior of "the MethodBuilder"

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
                'return,
                LINENUMBER(1),
                RETURN
            ) MAXSTACK 2 MAXLOCALS 3
        ),
        //if parameter <0 => catch-Block defines return value       => 0
        //if parameter =0 => try-Block defines return value         => 1
        //if parameter >0 => finally-Block  defines return value    => 2
        PUBLIC("tryCatchFinallyTest", "(I)", "I")(
            CODE(
                ICONST_1,
                ISTORE_2,
                TRY('Try1),
                TRY('FinallyTry2),
                TRY('LastPCTry3),
                ILOAD_1,
                IFGE('tryEnd),
                NEW("java/lang/Exception"),
                DUP,
                INVOKESPECIAL("java/lang/Exception", false, "<init>", "()V"),
                ATHROW,
                'tryEnd,
                TRYEND('Try1),
                GOTO('finally),
                CATCH('Try1, "java/lang/Exception"),
                POP,
                ICONST_0,
                ISTORE_2,
                TRYEND('FinallyTry2),
                GOTO('finally),
                CATCH('FinallyTry2),
                CATCH('LastPCTry3),
                POP,
                'finally,
                ILOAD_1,
                IFLE('return),
                ICONST_2,
                ISTORE_2,
                'return,
                ILOAD_2,
                IRETURN,
                TRYEND('LastPCTry3)
            ) MAXLOCALS 3
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

    it should "have a LineNumberTable" in {
        val lineNumberTable = attributeTestMethod.body.get.attributes.collect {
            case l: br.LineNumberTable ⇒ l
        }.head
        assert(lineNumberTable.lookupLineNumber(4).get == 1)
    }

    "the generated method `tryCatchFinallyTest`" should "have the correct exceptionTable set" in {
        val exceptionTable = attributeBrClassFile.methods.find {
            m ⇒ m.name == "tryCatchFinallyTest"
        }.get.body.get.exceptionHandlers

        println(exceptionTable)

        assert(
            exceptionTable.contains(
                br.ExceptionHandler(2, 14, 17, Some(br.ObjectType("java/lang/Exception")))
            )
        )
        assert(exceptionTable.contains(br.ExceptionHandler(2, 20, 23, None)))
        assert(exceptionTable.contains(br.ExceptionHandler(2, 32, 23, None)))
    }

    "the generated method `tryCatchFinallyTest`" should "execute as expected" in {
        val attributeTestInstance = loader.loadClass("AttributeMethodClass").newInstance()
        val mirror = runtimeMirror(loader).reflect(attributeTestInstance)
        val method = mirror.symbol.typeSignature.member(TermName("tryCatchFinallyTest")).asMethod

        assert(mirror.reflectMethod(method)(-1) == 0)
        assert(mirror.reflectMethod(method)(0) == 1)
        assert(mirror.reflectMethod(method)(1) == 2)
    }

}
