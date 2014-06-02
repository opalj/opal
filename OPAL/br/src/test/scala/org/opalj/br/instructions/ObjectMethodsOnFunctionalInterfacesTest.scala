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
import reader.{ Java8Framework, Java8LibraryFramework }

/**
 * Tests that calls to inherited methods on lambda instances go to Object.
 *
 * @author Arne Lottmann
 */
class ObjectMethodsOnFunctionalInterfacesTest extends FunSpec with Matchers {

    val InvokedMethod = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedMethod")

    val testResources = TestSupport.locateTestResources("classfiles/Lambdas.jar", "br")
    val rtJar = new File(TestSupport.JRELibraryFolder, "rt.jar")

    val project: SomeProject = Project(
        Java8Framework.ClassFiles(testResources),
        Java8LibraryFramework.ClassFiles(rtJar))

    private def testMethod(classFile: ClassFile, name: String) {
        for {
            method @ MethodWithBody(body) ← classFile.findMethod(name)
            instruction ← body.instructions if instruction.isInstanceOf[INVOKEVIRTUAL]
            invokevirtual = instruction.asInstanceOf[INVOKEVIRTUAL]
            annotations = method.runtimeVisibleAnnotations
        } {
            val invokedMethod = getInvokedMethod(annotations)
            invokedMethod should be('defined)

            val methodIdentifier = invokedMethod.get.toJava
            it("«"+methodIdentifier+"» should resolve to Object's method") {
                val declaringClass = invokevirtual.declaringClass
                declaringClass should be('ObjectType)
                val declaringClassFile = project.classFile(declaringClass.asObjectType)
                declaringClassFile should be('defined)
                val actualName = invokevirtual.name
                val actualDescriptor = invokevirtual.methodDescriptor
                val actualMethod = declaringClassFile.flatMap(
                    _.findMethod(actualName, actualDescriptor))
                actualMethod should be('defined)
                actualMethod should be(invokedMethod)
            }
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
    private def getInvokedMethod(annotations: Annotations): Option[Method] = {
        val candidates: IndexedSeq[Option[Method]] = for {
            invokedMethod ← annotations.filter(_.annotationType == InvokedMethod)
            pairs = invokedMethod.elementValuePairs
            ElementValuePair("receiverType", ClassValue(receiverType)) ← pairs
            ElementValuePair("name", StringValue(methodName)) ← pairs
            classFile ← project.classFile(receiverType.asObjectType)
        } yield {
            val parameterTypes = getParameterTypes(pairs)
            val returnType = getReturnType(pairs)
            val descriptor = MethodDescriptor(parameterTypes, returnType)
            classFile.findMethod(methodName, descriptor)
        }
        if (candidates.nonEmpty) candidates.head else None
    }

    private def getParameterTypes(pairs: ElementValuePairs): IndexedSeq[FieldType] = {
        pairs.find(_.name == "parameterTypes").map { p ⇒
            p.value.asInstanceOf[ArrayValue].values.map(_ match {
                case ClassValue(x: ObjectType) ⇒ x
                case ClassValue(x: BaseType)   ⇒ x
                case x: ElementValue           ⇒ x.valueType
            })
        }.getOrElse(IndexedSeq())
    }

    private def getReturnType(pairs: ElementValuePairs): Type = {
        pairs.find(_.name == "returnType").map { p ⇒
            p.value.asInstanceOf[ClassValue].value
        }.getOrElse(VoidType)
    }

    describe("Invocations of inherited methods on instances of functional interfaces") {
        val testClass = project.classFile(
            ObjectType("lambdas/ObjectMethodsOnFunctionalInterfaces")).get
        val annotatedMethods = testClass.methods.filter(
            _.runtimeVisibleAnnotations.nonEmpty)
        annotatedMethods.foreach(m ⇒ testMethod(testClass, m.name))
    }
}
