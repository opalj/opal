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

import analyses.{ Project, SomeProject }
import org.scalatest.Matchers
import org.scalatest.FunSpec
import java.io.File
import org.opalj.br.reader.Java8Framework

/**
 * Test resolution capabilities of the [[INVOKEDYNAMIC]] instruction.
 *
 * @author Arne Lottmann
 */
class InvokedynamicTest extends FunSpec with Matchers {

    val InvokedMethod = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedMethod")

    val testResources = TestSupport.locateTestResources("classfiles/Lambdas.jar", "br")

    val project: SomeProject = Project(testResources)

    private def testMethod(classFile: ClassFile, name: String) {
        for {
            method @ MethodWithBody(body) ← classFile.findMethod(name)
            instruction ← body.instructions if instruction.isInstanceOf[INVOKEDYNAMIC]
            invokedynamic = instruction.asInstanceOf[INVOKEDYNAMIC]
            annotations = method.runtimeVisibleAnnotations
        } {
            val expectedTarget = getInvokedMethod(annotations)
            expectedTarget should be('defined)

            val actualTarget = invokedynamic.resolveJDK8(project)
            actualTarget should be('defined)
            actualTarget should be(expectedTarget)
        }
    }

    /**
     * This method retrieves '''the first''' "invoked method" that is specified by an
     * [[InvokedMethod]] annotation present in the given annotations.
     *
     * This assumes that in the test cases, there is never more than one [[InvokedMethod]]
     * annotation on a single test method.
     *
     * The InvokedMethod annotation might have to be revised for use with Java 8 lambdas,
     * or used multiple times (the first time referring to the actual generated
     * invokedynamic instruction, while all other times would refer to invocations of the
     * generated object's single method).
     */
    private def getInvokedMethod(annotations: Annotations): Option[Method] =
        (
            for {
                invokedMethod ← annotations.filter(_.annotationType == InvokedMethod)
                pairs = invokedMethod.elementValuePairs
                ElementValuePair("receiverType", ClassValue(receiverType)) ← pairs
                ElementValuePair("name", StringValue(methodName)) ← pairs
                classFile ← project.classFile(receiverType.asObjectType)
            } yield {
                classFile.findMethod(methodName)
            }
        ).head

    describe("An INVOKEDYNAMIC instruction") {
        describe("when handling Java 8 lambda expressions") {
            val Lambdas = project.classFile(ObjectType("lambdas/Lambdas")).get

            it("should resolve a parameterless lambda") {
                testMethod(Lambdas, "plainLambda")
            }

            it("should resolve a lambda with a reference to a local variable") {
                testMethod(Lambdas, "localClosure")
            }

            it("should resolve a lambda with a reference to an instance variable") {
                testMethod(Lambdas, "instanceClosure")
            }

            it("should resolve a lambda with references to both local and instance variables") {
                testMethod(Lambdas, "localAndInstanceClosure")
            }
        }

        describe("when handling Java 8 method references") {
            val MethodReferences = project.classFile(ObjectType("lambdas/MethodReferences")).get

            it("should resolve a reference to a static method") {
                testMethod(MethodReferences, "compareValues")
            }

            it("should resolve a reference to an instance method") {
                testMethod(MethodReferences, "filterOutEmptyValues")
            }
        }

        describe("when passed the jre 8 jars") {
            it("should resolve all invokedynamic instructions found there") {
                val jrePath = TestSupport.JRELibraryFolder
                val jreProject = Project(jrePath)
                val failedInstructions = (for {
                    classFile ← jreProject.classFiles
                    method @ MethodWithBody(body) ← classFile.methods
                    instruction ← body.instructions if instruction.isInstanceOf[INVOKEDYNAMIC]
                    invokedynamic = instruction.asInstanceOf[INVOKEDYNAMIC]
                } yield {
                    (invokedynamic.resolveJDK8(jreProject).isDefined,
                        classFile, method, instruction)
                }).filter(t ⇒ !t._1)
                if (!failedInstructions.isEmpty) {
                    val totalFailures = failedInstructions.size
                    val numberOfFailuresToShow = 5
                    val msg = failedInstructions.take(numberOfFailuresToShow).map({ tuple ⇒
                        val (_, classFile, method, instruction) = tuple
                        instruction+"\n in method "+
                            method.toJava
                    }).mkString(
                        "Failed to resolve the following instructions:\n",
                        "\n",
                        "\nand "+(totalFailures - numberOfFailuresToShow)+" more.")
                    fail(msg)
                }
            }
        }
    }
}
