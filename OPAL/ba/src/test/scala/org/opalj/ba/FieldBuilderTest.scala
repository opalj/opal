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

import java.io.ByteArrayInputStream

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import scala.reflect.runtime.universe._

import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bc.Assembler
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the properties of fields build with the BytecodeAssembler DSL. The class is build,
 * assembled as a [[org.opalj.da.ClassFile]] and read again as a [[org.opalj.br.ClassFile]]. It is
 * also loaded, instantiated and executed with the JVM.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class FieldBuilderTest extends FlatSpec {

    behavior of "Fields"

    val binaryClassName = "test/FieldClass"
    val (daClassFile, _) =
        CLASS(
            accessModifiers = SUPER PUBLIC,
            thisType = binaryClassName,
            fields = FIELDS(
                FIELD(FINAL PUBLIC, "publicField", "I"),
                FIELD(PRIVATE, "privateField", "Z")
            ),
            methods = METHODS(
                METHOD(PUBLIC, "<init>", "()V",
                    CODE(
                        ALOAD_0,
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        ALOAD_0,
                        ICONST_3,
                        PUTFIELD("test/FieldClass", "publicField", "I"),
                        ALOAD_0,
                        ICONST_1,
                        PUTFIELD("test/FieldClass", "privateField", "Z"),
                        RETURN
                    )),
                METHOD(PUBLIC, "packageField", "()Z",
                    CODE(
                        ALOAD_0,
                        GETFIELD("test/FieldClass", "privateField", "Z"),
                        IRETURN
                    )),
                METHOD(
                    PUBLIC, "publicField", "()I",
                    CODE(
                        ALOAD_0,
                        GETFIELD("test/FieldClass", "publicField", "I"),
                        IRETURN
                    )
                )
            )
        ).toDA()

    val rawClassFile = Assembler(daClassFile)
    val javaClassName = binaryClassName.replace('/', '.')
    val loader = new InMemoryClassLoader(
        Map(javaClassName → rawClassFile),
        this.getClass.getClassLoader
    )

    val fieldInstance = loader.loadClass(javaClassName).getDeclaredConstructor().newInstance()
    val mirror = runtimeMirror(loader).reflect(fieldInstance)

    val brClassFile = Java8Framework.ClassFile(() ⇒ new ByteArrayInputStream(rawClassFile)).head

    def getField(name: String) = brClassFile.fields.find(f ⇒ f.name == name).get

    "the fields in `FieldClass`" should "have the correct visibility modifiers" in {
        assert(getField("privateField").accessFlags == ACC_PRIVATE.mask)
        assert(getField("publicField").accessFlags == (ACC_PUBLIC.mask | ACC_FINAL.mask))
    }

    "the field `FieldClass.privateField`" should "be initialized as true" in {
        val field = mirror.symbol.typeSignature.member(TermName("privateField")).asTerm
        assert(mirror.reflectField(field).get == true)
    }

    "FieldClass.publicField" should "be initialized as 3" in {
        val field = mirror.symbol.typeSignature.member(TermName("publicField")).asTerm
        assert(mirror.reflectField(field).get == 3)
    }

}
