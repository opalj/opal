/* License (BSD Style License):
 * Copyright (c) 2009, 2011
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import de.tud.cs.st.bat.TestSupport
import de.tud.cs.st.bat.resolved.Annotation
import de.tud.cs.st.bat.resolved.AnnotationValue
import de.tud.cs.st.bat.resolved.ArrayValue
import de.tud.cs.st.bat.resolved.ClassValue
import de.tud.cs.st.bat.resolved.ElementValuePair
import de.tud.cs.st.bat.resolved.IntValue
import de.tud.cs.st.bat.resolved.Method
import de.tud.cs.st.bat.resolved.StringValue
import de.tud.cs.st.bat.resolved.ai.project.CHACallGraphAlgorithmConfiguration
import de.tud.cs.st.bat.resolved.ai.project.CallGraphAlgorithmConfiguration
import de.tud.cs.st.bat.resolved.ai.project.CallGraphFactory
import de.tud.cs.st.bat.resolved.reader.Java7Framework

/**
 * Tests a callgraph implementation using the classes in CallGraph.jar
 *
 * @author Marco Jacobasch
 */
abstract class AbstractCallGraphTest extends FlatSpec with Matchers {

    behavior of "BATAICALLGRAPH"

    import Console._

    //
    // Override to specify other fixtures or callgraph algorithms
    //
    def testFileName: String
    def testFilePath: String
    def testCallGraphAlgorithm: CallGraphAlgorithmConfiguration[Any]

    //
    // ANNOTATIONTYPES
    // 
    val invokedMethodAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/InvokedMethod")
    val invokedMethodsAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/InvokedMethods")

    val invokedConstructorAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/InvokedConstructor")
    val invokedConstructorsAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/InvokedConstructors")

    val accessedFieldAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/AccessedField")
    val accessedFieldsAnnotation = ObjectType("de/tud/cs/st/bat/test/invokedynamic/annotations/AccessedFields")

    //
    // PROJECT SETUP
    //
    def file = TestSupport.locateTestResources(testFileName, testFilePath)
    val classFiles = Java7Framework.ClassFiles(file)
    val project = bat.resolved.analyses.IndexBasedProject(classFiles)

    //
    // GRAPH CONSTRUCTION
    //
    val (callGraph, unresolvedMethodCalls, exceptions) = CallGraphFactory.create(
        project,
        CallGraphFactory.defaultEntryPointsForLibraries(project),
        new CHACallGraphAlgorithmConfiguration())

    //
    // UTILITY FUNCTIONS
    //

    // Single Method Test
    def singleMethodTest(method: Method, annotation: Annotation) = {
        val evps = annotation.elementValuePairs
        val Some(fqnClass) = evps collectFirst ({ case ElementValuePair("receiverType", ClassValue(fqn)) ⇒ fqn })
        val Some(methodName) = evps collectFirst ({ case ElementValuePair("name", StringValue(name)) ⇒ name })
        val Some(lineNumber) = evps collectFirst ({ case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })

        val zippedAndFlattenedCallGraph = callGraph.calls(method).map { f ⇒
            f._2.view.zipWithIndex map { case (value, index) ⇒ (f._1, value) }
        }.flatten
        zippedAndFlattenedCallGraph.size should be > 0

        val filteredCallGraph = zippedAndFlattenedCallGraph filter { f ⇒
            f._2.name.equals(methodName) &&
                project.classFile(f._2).thisType.equals(fqnClass)
        }
        filteredCallGraph.size should be > 0

        val lineNumberFilteredCallGraph = filteredCallGraph filter { f ⇒
            val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(f._1)
            line == lineNumber
        }
        lineNumberFilteredCallGraph.size should be <= 1

        lineNumberFilteredCallGraph foreach { f ⇒
            f._2.name should be(methodName)
            project.classFile(f._2).thisType should be(fqnClass)
        }
    }

    // Single Constructor Test
    def singleConstructorTest(method: Method, annotation: Annotation) = {
        val evps = annotation.elementValuePairs
        val Some(fqnClass) = evps collectFirst ({ case ElementValuePair("receiverType", ClassValue(fqn)) ⇒ fqn })
        val Some(lineNumber) = evps collectFirst ({ case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })

        val zippedAndFlattenedCallGraph = callGraph.calls(method).map { f ⇒
            f._2.view.zipWithIndex map { case (value, index) ⇒ (f._1, value) }
        }.flatten
        zippedAndFlattenedCallGraph.size should be > 0

        val filteredCallGraph = zippedAndFlattenedCallGraph filter { f ⇒
            f._2.name.equals("<init>") &&
                project.classFile(f._2).thisType.equals(fqnClass)
        }
        filteredCallGraph.size should be > 0

        val lineNumberFilteredCallGraph = filteredCallGraph filter { f ⇒
            val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(f._1)
            line == lineNumber
        }
        lineNumberFilteredCallGraph.size should be <= 1

        lineNumberFilteredCallGraph foreach { f ⇒
            f._2.name should be("<init>")
            project.classFile(f._2).thisType should be(fqnClass)
        }
    }

    // Single Field Access Test
    def singleFieldAccessTest(method: Method, annotation: Annotation) = {
        val evps = annotation.elementValuePairs
        val Some(fqnClass) = evps collectFirst ({ case ElementValuePair("declaringType", ClassValue(declaringType)) ⇒ declaringType })
        val Some(fieldType) = evps collectFirst ({ case ElementValuePair("fieldType", ClassValue(fieldType)) ⇒ fieldType })
        val Some(fieldName) = evps collectFirst ({ case ElementValuePair("name", StringValue(name)) ⇒ name })
        val Some(lineNumber) = evps collectFirst ({ case ElementValuePair("lineNumber", IntValue(lineNumber)) ⇒ lineNumber })
    }

    //
    // TESTS
    //

    // Validate every method against the callgraph defined by annotations
    for {
        classFile ← project.classFiles
        method ← classFile.methods
    } {
        it should "find a superset of the callgraph for method "+method+" in class "+classFile.fqn in {

            // single InvokedMethod per method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (invokedMethodAnnotation)) foreach (singleMethodTest(method, _))

            // multiple InvokedMethod per Method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (invokedMethodsAnnotation)) foreach { f ⇒
                    val Some(annotationArray) = f.elementValuePairs collectFirst ({ case ElementValuePair("value", ArrayValue(array)) ⇒ array })
                    val annotations = annotationArray collect ({ case AnnotationValue(annotation) ⇒ annotation })
                    annotations foreach (singleMethodTest(method, _))
                }

            // single InvokedConstructor per method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (invokedConstructorAnnotation)) foreach (singleConstructorTest(method, _))

            // multiple InvokedConstructor per method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (invokedConstructorsAnnotation)) foreach { f ⇒
                    val Some(annotationArray) = f.elementValuePairs collectFirst ({ case ElementValuePair("value", ArrayValue(array)) ⇒ array })
                    val annotations = annotationArray collect ({ case AnnotationValue(annotation) ⇒ annotation })
                    annotations foreach (singleConstructorTest(method, _))
                }

            // single AccessedField per method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (accessedFieldAnnotation)) foreach (singleFieldAccessTest(method, _))

            // multiple AccessedField per method
            method.runtimeVisibleAnnotations filter (
                _.annotationType equals (accessedFieldsAnnotation)) foreach { f ⇒
                    val Some(annotationArray) = f.elementValuePairs collectFirst ({ case ElementValuePair("value", ArrayValue(array)) ⇒ array })
                    val annotations = annotationArray collect ({ case AnnotationValue(annotation) ⇒ annotation })
                    annotations foreach (singleFieldAccessTest(method, _))
                }

        }
    }

}