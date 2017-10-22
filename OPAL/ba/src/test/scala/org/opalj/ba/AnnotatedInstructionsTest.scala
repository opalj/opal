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

    {
        behavior of "Instructions annotated with Strings"

        val brClassTemplate: CLASS[(Map[org.opalj.br.PC, String], List[String])] = CLASS(
            accessModifiers = PUBLIC SUPER,
            thisType = "Test",
            methods = METHODS(
                METHOD(PUBLIC, "<init>", "()V", CODE(
                    'UnUsedLabel1,
                    ALOAD_0 → "MarkerAnnotation1",
                    'UnUsedLabel2,
                    INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                    RETURN → "MarkerAnnotation2"
                ))
            )
        )
        val (
            daClassFile,
            methodAnnotations: Map[br.Method, (Map[br.PC, String], List[String])]
            ) = brClassTemplate.toDA()
        val (pcAnnotations: List[Map[br.PC, String]], warnings) =
            methodAnnotations.values.unzip

        "[String Annotated Instructions] the class generation" should "have no warnings" in {
            assert(warnings.flatten.isEmpty)
        }

        "[String Annotated Instructions] the generated class" should "load correctly" in {
            val loader = new InMemoryClassLoader(
                Map("Test" → Assembler(daClassFile)), this.getClass.getClassLoader
            )
            assert("Test" == loader.loadClass("Test").getSimpleName)
        }

        "[String Annotated Instructions] the method " should "have the correct annotations" in {
            assert(pcAnnotations.head(0) == "MarkerAnnotation1")
            assert(pcAnnotations.head(4) == "MarkerAnnotation2")
        }
    }

    {
        behavior of "Instructions annotated with Tuples"

        val (
            daClassFile,
            methodAnnotations: Map[br.Method, (Map[br.PC, (Symbol, String)], List[String])]
            ) =
            CLASS(
                accessModifiers = PUBLIC SUPER,
                thisType = "Test",
                methods = METHODS(
                    METHOD(PUBLIC, "<init>", "()V", CODE(
                        'UnUsedLabel1,
                        ALOAD_0 → (('L1, "MarkerAnnotation1")),
                        'UnUsedLabel2,
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        RETURN → (('L2, "MarkerAnnotation2"))
                    ))
                )
            ).toDA()
        val (pcAnnotations: List[Map[br.PC, (Symbol, String)]], warnings) =
            methodAnnotations.values.unzip

        "[Tuple Annotated Instructions] the class generation" should "have no warnings" in {
            assert(warnings.flatten.isEmpty)
        }

        "[Tuple Annotated Instructions] the generated class" should "load correctly" in {
            val loader = new InMemoryClassLoader(
                Map("Test" → Assembler(daClassFile)), this.getClass.getClassLoader
            )
            assert("Test" == loader.loadClass("Test").getSimpleName)
        }

        "[Tuple Annotated Instructions] the method " should "have the correct annotations" in {

            assert(pcAnnotations.head(0) == (('L1, "MarkerAnnotation1")))
            assert(pcAnnotations.head(4) == (('L2, "MarkerAnnotation2")))
        }
    }
}
