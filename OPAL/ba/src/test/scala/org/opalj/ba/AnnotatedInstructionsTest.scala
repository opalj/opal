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

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

import org.opalj.bc.Assembler
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.RETURN
import org.opalj.util.InMemoryClassLoader

/**
 * Tests annotating instructions in the BytecodeAssembler DSL.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class AnnotatedInstructionsTest extends FlatSpec {
    behavior of "Annotated Instructions"
    val testClass = (PUBLIC CLASS "Test" EXTENDS "java/lang/Object")(
        PUBLIC("<init>", "()", "V")(
            CODE(
                ALOAD_0 → "MarkerAnnotation1",
                INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                RETURN → "MarkerAnnotation2"
            )
        )
    )

    val (daClassFile, annotations) = testClass.buildDAClassFile
    val loader = new InMemoryClassLoader(
        Map("Test" → Assembler(daClassFile)),
        this.getClass.getClassLoader
    )
    import loader.loadClass

    "the generated class" should "load correctly" in {
        assert("Test" == loadClass("Test").getSimpleName)
    }

    "the method '<init>()V'" should "have the correct annotations" in {
        val (_, methodAnnotations) = annotations.head
        assert(methodAnnotations(0).asInstanceOf[String] == "MarkerAnnotation1")
        assert(methodAnnotations(4).asInstanceOf[String] == "MarkerAnnotation2")
    }

}
