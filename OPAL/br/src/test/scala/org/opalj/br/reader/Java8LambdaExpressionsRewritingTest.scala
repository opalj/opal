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
package br
package reader

import org.scalatest.Matchers
import org.scalatest.FunSpec

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory

import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext

import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.bi.TestSupport.{locateTestResources ⇒ locate}
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.INVOKESTATIC

/**
 * Tests the rewriting of Java 8 lambda expressions/method references based
 * [[org.opalj.br.instructions.INVOKEDYNAMIC]] instruction.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 * @author Andreas Muttscheller
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class Java8LambdaExpressionsRewritingTest extends FunSpec with Matchers {

    val InvokedMethod = ObjectType("annotations/target/InvokedMethod")

    val lambdas18Project = locate("lambdas-1.8-g-parameters-genericsignature.jar", "bi")

    private def testMethod(project: SomeProject, classFile: ClassFile, name: String): Unit = {
        var successful = false
        val methods = classFile.findMethod(name)
        for {
            (method, body) ← methods.collect { case method @ MethodWithBody(body) ⇒ (method, body) }
            factoryCall ← body.collectInstructions { case i: INVOKESTATIC ⇒ i }
            if factoryCall.declaringClass.fqn.matches("^Lambda\\$[A-Fa-f0-9]+:[A-Fa-f0-9]+$")
            annotations = method.runtimeVisibleAnnotations
        } {
            successful = true
            val expectedTarget = getInvokedMethod(project, annotations)
            if (expectedTarget.isEmpty) {
                val message =
                    annotations.
                        filter(_.annotationType == InvokedMethod).
                        mkString("\n\t", "\n\t", "\n")
                fail(
                    s"the specified invoked method $message is not defined "+
                        classFile.methods.map(_.name).mkString("; defined methods = {", ",", "}")
                )
            }

            val actualTarget = getCallTarget(project, factoryCall)
            withClue {
                s"failed to resolve $factoryCall in ${method.toJava(classFile)}"
            }(actualTarget should be(expectedTarget))
        }
        assert(successful, s"couldn't find factory method call in $name")
    }

    private def getCallTarget(project: SomeProject, factoryCall: INVOKESTATIC): Option[Method] = {
        val proxy = project.classFile(factoryCall.declaringClass).get
        val forwardingMethod = proxy.methods.find { m ⇒
            !m.isConstructor && m.name != factoryCall.name && !m.isBridge
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
                MethodDescriptor(invocationInstruction.methodDescriptor.parameterTypes, VoidType)
            } else {
                invocationInstruction.methodDescriptor
            }

        project.resolveMethodReference(
            declaringType.asObjectType,
            targetMethodName,
            targetMethodDescriptor
        )
    }

    /**
     * This method retrieves '''the first''' "invoked method" that is specified by an
     * [[InvokedMethod]] annotation present in the given annotations.
     *
     * This assumes that in the test cases, there is never more than one [[InvokedMethod]]
     * annotation on a single test method.
     *
     * The `InvokedMethod` annotation might have to be revised for use with Java 8 lambdas,
     * or used multiple times (the first time referring to the actual generated
     * invokedynamic instruction, while all other times would refer to invocations of the
     * generated object's single method).
     */
    private def getInvokedMethod(project: SomeProject, annotations: Annotations): Option[Method] = {
        val method = for {
            invokedMethod ← annotations.filter(_.annotationType == InvokedMethod)
            pairs = invokedMethod.elementValuePairs
            ElementValuePair("receiverType", StringValue(receiverType)) ← pairs
            ElementValuePair("name", StringValue(methodName)) ← pairs
            classFileOpt = project.classFile(ObjectType(receiverType))
        } yield {
            if (classFileOpt.isEmpty) {
                throw new IllegalStateException(s"the class file $receiverType cannot be found")
            }
            findMethodRecursive(project, classFileOpt.get, methodName, receiverType)
        }

        Some(method.head)
    }

    /**
     * Get the method definition recursively -> if the method isn't implemented in `classFile`, check if
     * the super class has an implementation.
     *
     * @param project The project where to look for the classfile
     * @param classFile The classfile to check the method
     * @param methodName The name of the method to find
     * @param receiverType The type of the receiver, which was defined in the fixture annotation
     * @return The `Method` with the name `methodName`
     */
    def findMethodRecursive(
        project:      SomeProject,
        classFile:    ClassFile,
        methodName:   String,
        receiverType: String
    ): Method = {
        /**
         * Get the method definition recursively -> if the method isn't implemented in `classFile`, check if
         * the super class has an implementation.
         *
         * @param classFile The classfile to check the method
         * @return An Option of the `Method`
         */
        def findMethodRecursiveInner(classFile: ClassFile): Method = {
            val methodOpt = classFile.findMethod(methodName)
            if (methodOpt.isEmpty) {
                classFile.superclassType match {
                    case Some(superType) ⇒ findMethodRecursiveInner(project.classFile(superType).get)
                    case None ⇒ throw new IllegalStateException(
                        s"$receiverType does not define $methodName"
                    )
                }
            } else {
                methodOpt.head
            }
        }
        findMethodRecursiveInner(classFile)
    }

    def testProject(project: SomeProject): Unit = {
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

        val MethodReferences = project.classFile(ObjectType("lambdas/methodreferences/MethodReferences")).get

        it("should resolve a reference to a static method") {
            testMethod(project, MethodReferences, "compareValues")
        }

        it("should resolve a reference to an instance method") {
            testMethod(project, MethodReferences, "filterOutEmptyValues")
        }

        it("should resolve a reference to a constructor") {
            testMethod(project, MethodReferences, "newValue")
        }

        val ReceiverInheritance = project.classFile(ObjectType("lambdas/methodreferences/ReceiverInheritance")).get

        it("should resolve a reference to a method implemented in a superclass") {
            testMethod(project, ReceiverInheritance, "instanceBiConsumer")
        }
    }

    describe("rewriting of Java 8 lambda expressions") {
        val cache = new BytecodeInstructionsCache
        implicit val logContext: LogContext = GlobalLogContext
        val baseConfig: Config = ConfigFactory.load()
        val rewritingConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsRewritingConfigKey
        val logRewritingsConfigKey = Java8LambdaExpressionsRewriting.Java8LambdaExpressionsLogRewritingsConfigKey
        val testConfig = baseConfig.
            withValue(rewritingConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.TRUE)).
            withValue(logRewritingsConfigKey, ConfigValueFactory.fromAnyRef(java.lang.Boolean.FALSE))
        class Framework extends {
            override val config = testConfig
        } with Java8FrameworkWithLambdaExpressionsSupportAndCaching(cache)
        val framework = new Framework()
        val project = Project(
            framework.ClassFiles(lambdas18Project),
            Java8LibraryFramework.ClassFiles(org.opalj.bytecode.JRELibraryFolder),
            true,
            Traversable.empty,
            Project.defaultHandlerForInconsistentProjects,
            testConfig,
            logContext
        )
        testProject(project)
    }
}
