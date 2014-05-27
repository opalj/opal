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
package ai
package project

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers

import br._
import br.reader.Java8Framework

/**
 * Tests a callgraph implementation using the classes in CallGraph.jar
 *
 * @author Marco Jacobasch
 */
@RunWith(classOf[JUnitRunner])
abstract class AbstractCallGraphTest extends FlatSpec with Matchers {

    behavior of "a computed Call Graph"

    // should be overridden by subclasses if the CallGraph also contains reflective calls
    def ignoreReflectiveCalls: Boolean = true

    import Console._

    //
    // Override to specify other fixtures or callgraph algorithms
    //
    def testFileName: String

    def testFilePath: String

    def testCallGraph: analyses.ProjectInformationKey[ComputedCallGraph]

    //
    // ANNOTATIONTYPES
    // 
    val invokedMethodAnnotation = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedMethod")
    val invokedMethodsAnnotation = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedMethods")

    val invokedConstructorAnnotation = ObjectType("org/opalj/ai/test/invokedynamic/annotations/InvokedConstructor")
    val invokedConstructorsAnnotation = ObjectType("org/opalj/ai/invokedynamic/annotations/InvokedConstructors")

    val accessedFieldAnnotation = ObjectType("org/opalj/ai/test/invokedynamic/annotations/AccessedField")
    val accessedFieldsAnnotation = ObjectType("org/opalj/ai/test/invokedynamic/annotations/AccessedFields")

    //
    // PROJECT SETUP
    //
    def file = TestSupport.locateTestResources(testFileName, testFilePath)
    val classFiles = Java8Framework.ClassFiles(file)
    val project = br.analyses.Project(classFiles)

    //
    // GRAPH CONSTRUCTION
    //
    val ComputedCallGraph(callGraph, unresolvedMethodCalls, exceptions) =
        project.get(testCallGraph)

    //
    // UTILITY FUNCTIONS
    //

    // Single Method Test
    def singleMethodTest(method: Method, annotation: Annotation): Unit = {
        val evps = annotation.elementValuePairs
        val Some(receiver) =
            evps collectFirst (
                { case ElementValuePair("receiverType", ClassValue(receiver)) ⇒ receiver })
        val Some(methodName) =
            evps collectFirst (
                { case ElementValuePair("name", StringValue(name)) ⇒ name })
        val Some(lineNumber) =
            evps collectFirst (
                { case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })
        val isReflective: Boolean =
            (evps collectFirst (
                { case ElementValuePair("isReflective", BooleanValue(isReflective)) ⇒ isReflective })
            ).getOrElse(false)

        val receiverClassIsUnknown = !project.classFile(receiver.asObjectType).isDefined

        // If we are not able to handle reflective calls and we have one, forget about it
        if (isReflective && ignoreReflectiveCalls)
            cancel("ignoring reflection based test")

        val callees = callGraph.calls(method).map { f ⇒
            val (pc, callees) = f
            callees map { ((pc, _)) }
        }.flatten

        if (callees.size == 0) {
            val className = project.classFile(method).fqn
            val message = className+" { "+method+" } has no callees; expected: "+annotation.toJava
            fail(message)
        }

        val calleeMatchingAnnotation = callees filter { f ⇒
            val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(f._1)
            f._2.name.equals(methodName) &&
                project.classFile(f._2).thisType.equals(receiver) &&
                line == lineNumber
        }
        
        val unresolvedReceiverCalleesWithMatchingAnnotation = unresolvedMethodCalls filter { call =>
            val Some(line) = method.body.get.lineNumber(call.pc)
            receiverClassIsUnknown && // Just for performance
            call.caller.equals(method) &&
            call.calleeName.equals(methodName) &&
            call.calleeClass.equals(receiver) &&
            line == lineNumber
        }

        if (calleeMatchingAnnotation.size < 1 && (!receiverClassIsUnknown || unresolvedReceiverCalleesWithMatchingAnnotation.size < 1)) {
            val className = project.classFile(method).fqn
            val message = className+" { "+method+" } has none of the specified callees; expected: "+annotation.toJava+
                "\n actual: "+callees.map(f ⇒ (method.body.get.lineNumber(f._1), f._2, project.classFile(f._2).thisType))
            fail(message)
        }

        calleeMatchingAnnotation foreach { f ⇒
            f._2.name should be(methodName)
            project.classFile(f._2).thisType should be(receiver)
        }
    }

    // Single Constructor Test
    def singleConstructorTest(method: Method, annotation: Annotation): Unit = {
        
        // RETHINK
        
        val evps = annotation.elementValuePairs
        val Some(receiver) =
            evps collectFirst (
                { case ElementValuePair("receiverType", ClassValue(receiver)) ⇒ receiver })
        val Some(lineNumber) =
            evps collectFirst (
                { case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })

        val isReflective: Boolean =
            (evps collectFirst (
                { case ElementValuePair("isReflective", BooleanValue(isReflective)) ⇒ isReflective })
            ).getOrElse(false)

        val receiverClassIsUnknown = !project.classFile(receiver.asObjectType).isDefined

        // If we are not able to handle reflective calls and we have one, forget about it
        if (isReflective && ignoreReflectiveCalls)
            cancel("ignoring reflection based test")
            

        val callees = callGraph.calls(method).map { f ⇒
            val (pc, callees) = f
            callees map { ((pc, _)) }
        }.flatten

        if (callees.size == 0) {
            val className = project.classFile(method).fqn
            val message = className+" { "+method+" } has no called constructors; expected: "+annotation.toJava
            fail(message)
        }

        val calleeMatchingAnnotation = callees filter { f ⇒
            val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(f._1)
            f._2.name.equals("<init>") &&
                project.classFile(f._2).thisType.equals(receiver) &&
                line == lineNumber
        }

        val unresolvedReceiverCalleesWithMatchingAnnotation = unresolvedMethodCalls filter { call ⇒
            val Some(line) = method.body.get.lineNumber(call.pc)
            receiverClassIsUnknown && // Just for performance
                call.caller.equals(method) &&
                call.calleeName.equals("<init>") &&
                call.calleeClass.equals(receiver) &&
                line == lineNumber
        }

        if (calleeMatchingAnnotation.size < 1 && (!receiverClassIsUnknown || unresolvedReceiverCalleesWithMatchingAnnotation.size < 1)) {
            val className = project.classFile(method).fqn
            val message = className+" { "+method+" } has none of the specified constructor calls; expected: "+annotation.toJava +
                "\n actual: "+callees.map(f ⇒ (method.body.get.lineNumber(f._1), f._2, project.classFile(f._2).thisType))
            fail(message)
        }

        calleeMatchingAnnotation foreach { f ⇒
            f._2.name should be("<init>")
            project.classFile(f._2).thisType should be(receiver)
        }
    }

    // Single Field Access Test
    def singleFieldAccessTest(method: Method, annotation: Annotation): Unit = {
        val evps = annotation.elementValuePairs
        val (fqnClass) =
            evps collectFirst (
                {
                    case ElementValuePair("declaringType", ClassValue(declaringType)) ⇒
                        declaringType
                })

        val Some(fieldType) =
            evps collectFirst (
                { case ElementValuePair("fieldType", ClassValue(fieldType)) ⇒ fieldType })

        val Some(fieldName) =
            evps collectFirst (
                { case ElementValuePair("name", StringValue(name)) ⇒ name })

        val Some(lineNumber) =
            evps collectFirst (
                { case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })

        // TODO evaluate the result!
    }

    //
    // TESTS
    //

    // Validate every method against the callgraph defined by annotations
    for {
        classFile ← project.classFiles
        method ← classFile.methods
    } {
        it should ("correctly identify all call targets for the method "+
            method.toJava+" in class "+classFile.fqn) in {

                // single invocation per method
                method.runtimeVisibleAnnotations filter { annotation ⇒
                    annotation.annotationType == invokedMethodAnnotation
                } foreach { invokedMethod ⇒
                    singleMethodTest(method, invokedMethod)
                }

                // multiple invocations per Method
                method.runtimeVisibleAnnotations filter { annotation ⇒
                    annotation.annotationType == invokedMethodsAnnotation
                } foreach { f ⇒
                    val Some(annotationArray) =
                        f.elementValuePairs collectFirst {
                            { case ElementValuePair("value", ArrayValue(array)) ⇒ array }
                        }
                    annotationArray foreach { anInvokedMethod ⇒
                        val AnnotationValue(invokedMethod) = anInvokedMethod
                        singleMethodTest(method, invokedMethod)
                    }
                }

                // single constructor call per method
                method.runtimeVisibleAnnotations filter {
                    _.annotationType equals (invokedConstructorAnnotation)
                } foreach (singleConstructorTest(method, _))

                // multiple constructor calls per method
                method.runtimeVisibleAnnotations filter (
                    _.annotationType equals (invokedConstructorsAnnotation)) foreach { f ⇒
                        val Some(annotationArray) =
                            f.elementValuePairs collectFirst (
                                { case ElementValuePair("value", ArrayValue(array)) ⇒ array }
                            )
                        val annotations =
                            annotationArray collect (
                                { case AnnotationValue(annotation) ⇒ annotation }
                            )
                        annotations foreach (singleConstructorTest(method, _))
                    }

                // single field access per method
                method.runtimeVisibleAnnotations filter {
                    _.annotationType equals (accessedFieldAnnotation)
                } foreach (singleFieldAccessTest(method, _))

                // multiple field accesses per method
                method.runtimeVisibleAnnotations filter (
                    _.annotationType equals (accessedFieldsAnnotation)) foreach { f ⇒
                        val Some(annotationArray) =
                            f.elementValuePairs collectFirst (
                                { case ElementValuePair("value", ArrayValue(array)) ⇒ array }
                            )
                        val annotations =
                            annotationArray collect (
                                { case AnnotationValue(annotation) ⇒ annotation }
                            )
                        annotations foreach (singleFieldAccessTest(method, _))
                    }
            }
    }
}