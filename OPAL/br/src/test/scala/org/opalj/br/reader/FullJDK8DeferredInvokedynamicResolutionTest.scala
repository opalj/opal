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
package reader

import analyses.{ Project, SomeProject }
import org.scalatest.Matchers
import org.scalatest.FunSpec
import org.opalj.br.ClassFile
import org.opalj.br.ClassValue
import org.opalj.br.ElementValuePair
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.StringValue
import org.opalj.bi.TestSupport
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC

/**
 * Test resolution capabilities of the [[INVOKEDYNAMIC]] instruction.
 *
 * @author Arne Lottmann
 */
class FullJDK8DeferredInvokedynamicResolutionTest extends FunSpec with Matchers {

    val InvokedMethod = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedMethod")

    val testResources = TestSupport.locateTestResources("classfiles/Lambdas.jar", "br")

    private def testMethod(project: SomeProject, classFile: ClassFile, name: String) {
        for {
            method @ MethodWithBody(body) ← classFile.findMethod(name)
            instruction ← body.instructions
            if instruction.isInstanceOf[INVOKESTATIC]
            factoryCall = instruction.asInstanceOf[INVOKESTATIC]
            if factoryCall.declaringClass.fqn.matches("^Lambda\\$\\d+:\\d+$")
            annotations = method.runtimeVisibleAnnotations
        } {
            val expectedTarget = getInvokedMethod(project, annotations)
            expectedTarget should be('defined)

            val actualTarget = getCallTarget(project, factoryCall)
            withClue {
                s"Failed to resolve $factoryCall in ${classFile.fqn}.${method.toJava}"
            }(actualTarget should be(expectedTarget))
        }
    }

    private def getCallTarget(project: SomeProject, factoryCall: INVOKESTATIC): Option[Method] = {
        val proxy = project.classFile(factoryCall.declaringClass).get
        val forwardingMethod = proxy.methods.find { m ⇒
            !m.isConstructor && m.name != factoryCall.name &&
                !bi.ACC_BRIDGE.isSet(m.accessFlags)
        }.get
        val invocationInstructions = forwardingMethod.body.get.instructions.collect {
            case i: MethodInvocationInstruction ⇒ i
        }
        val invocationInstruction = invocationInstructions.head
        // declaringClass must be an ObjectType, since lambdas cannot be created on
        // array types, nor do arrays have methods that could be referenced
        val declaringType = invocationInstruction.declaringClass.asObjectType
        val targetMethodName = invocationInstruction.name
        val targetMethodDescriptor: MethodDescriptor =
            if (targetMethodName == "<init>") {
                MethodDescriptor(
                    invocationInstruction.methodDescriptor.parameterTypes,
                    VoidType
                )
            } else {
                invocationInstruction.methodDescriptor
            }
        project.classFile(declaringType).flatMap(_.
            findMethod(targetMethodName, targetMethodDescriptor))
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
    private def getInvokedMethod(project: SomeProject, annotations: Annotations): Option[Method] =
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

    def testProject(project: SomeProject) {
        val Lambdas = project.classFile(ObjectType("lambdas/Lambdas")).get

        it("should resolve a parameterless lambda") {
            testMethod(project, Lambdas, "plainLambda")
        }

        it("should resolve a lambda with a reference to a local variable") {
            testMethod(project, Lambdas, "localClosure")
        }

        it("should resolve a lambda with a reference to an instance variable") {
            testMethod(project, Lambdas, "instanceClosure")
        }

        it("should resolve a lambda with references to both local and instance variables") {
            testMethod(project, Lambdas, "localAndInstanceClosure")
        }

        val MethodReferences = project.classFile(ObjectType("lambdas/MethodReferences")).get

        it("should resolve a reference to a static method") {
            testMethod(project, MethodReferences, "compareValues")
        }

        it("should resolve a reference to an instance method") {
            testMethod(project, MethodReferences, "filterOutEmptyValues")
        }

        it("should resolve a reference to a constructor") {
            testMethod(project, MethodReferences, "newValue")
        }
    }

    describe("JDK 8 invokedynamic resolution in a regular framework") {
        val project: SomeProject = Project(
            Java8FrameworkWithLambdaSupport.ClassFiles(testResources),
            Java8LibraryFramework.ClassFiles(util.JRELibraryFolder)
        )
        testProject(project)
    }

    describe("JDK 8 invokedynamic resolution in a caching framework") {
        val cache = new BytecodeInstructionsCache
        val framework = new Java8FrameworkWithLambdaSupportAndCaching(cache)
        val project = Project(
            framework.ClassFiles(testResources),
            Java8LibraryFramework.ClassFiles(util.JRELibraryFolder)
        )
        testProject(project)
    }
}
