/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.br._
import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm

/**
 * Tests a callgraph implementation using the classes in CallGraph.jar
 *
 * @author Marco Jacobasch
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
abstract class AbstractCallGraphTest extends FlatSpec with Matchers {

    // should be overridden by subclasses if the CallGraph also contains reflective calls
    val ignoreReflectiveCalls: Boolean = true

    val ignoreCallTargetsOutsideOfProject: Boolean = true

    //
    // Override to specify other fixtures or callgraph algorithms
    //
    def testFileName: String

    def testFilePath: String

    def testCallGraphConfiguration: CallGraphAlgorithmConfiguration

    def testCallGraphAlgorithm: CallGraphAlgorithm

    val CGKey = ObjectType("annotations/target/CallGraphAlgorithm")

    //
    // ANNOTATIONTYPES
    //
    val invokedMethodAnnotation = ObjectType("annotations/target/InvokedMethod")
    val invokedMethodsAnnotation = ObjectType("annotations/target/InvokedMethods")

    val invokedConstructorAnnotation = ObjectType("annotations/target/InvokedConstructor")
    val invokedConstructorsAnnotation = ObjectType("annotations/target/InvokedConstructors")

    val accessedFieldAnnotation = ObjectType("annotations/target/AccessedField")
    val accessedFieldsAnnotation = ObjectType("annotations/target/AccessedFields")

    //
    // PROJECT SETUP
    //
    def file = org.opalj.bi.TestResources.locateTestResources(testFileName, testFilePath)
    val project = org.opalj.br.analyses.Project(file)

    //
    // GRAPH CONSTRUCTION
    //
    val ComputedCallGraph(callGraph, unresolvedMethodCalls, exceptions) = {
        val entryPoints = () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(project)
        CallGraphFactory.create(project, entryPoints, testCallGraphConfiguration)
    }

    //
    // UTILITY FUNCTIONS
    //

    // Single Method Test
    def singleMethodTest(method: Method, annotation: Annotation): Unit = {
        val evps = annotation.elementValuePairs
        val Some(receiver) =
            evps collectFirst {
                case ElementValuePair("receiverType", StringValue(receiver)) ⇒ ObjectType(receiver)
            }
        val Some(methodName) =
            evps collectFirst { case ElementValuePair("name", StringValue(name)) ⇒ name }
        val Some(lineNumber) =
            evps collectFirst {
                case ElementValuePair("line", IntValue(lineNumber)) ⇒ lineNumber
            }
        val isReflective: Boolean =
            (
                evps collectFirst {
                    case ElementValuePair("isReflective", BooleanValue(isReflective)) ⇒
                        isReflective
                }
            ).getOrElse(false)

        val isContainedIn =
            evps collectFirst {
                case ElementValuePair("isContainedIn", ArrayValue(isContainedIn)) ⇒
                    isContainedIn collect {
                        case (EnumValue(CGKey, "CHA"))        ⇒ CallGraphAlgorithm.CHA
                        case (EnumValue(CGKey, "DefaultVTA")) ⇒ CallGraphAlgorithm.DefaultVTA
                        case (EnumValue(CGKey, "BasicVTA"))   ⇒ CallGraphAlgorithm.BasicVTA
                        case (EnumValue(CGKey, "ExtVTA"))     ⇒ CallGraphAlgorithm.ExtVTA
                    }
            }

        val receiverClassIsUnknown = project.classFile(receiver).isEmpty
        val algorithmSpecified = isContainedIn.isDefined
        val preciselyResolvable = algorithmSpecified && isContainedIn.get.contains(testCallGraphAlgorithm)

        // If we are not able to handle reflective calls and we have one, forget about it
        if (isReflective && ignoreReflectiveCalls)
            cancel("ignoring reflection based test")

        if (project.classFile(receiver).isEmpty && ignoreCallTargetsOutsideOfProject)
            cancel("call target is not within the currently analyzed project")

        //    if (algorithmSpecified && !preciselyResolvable)
        //      cancel("call target is not precisly resolvable via the the " + testCallGraphAlgorithm)

        val callees =
            callGraph.calls(method).flatMap { calleesPerPC ⇒
                val (pc, callees) = calleesPerPC
                callees map { ((pc, _)) }
            }

        if (callees.isEmpty) {
            val className = method.classFile.fqn
            val message = className+" { "+method+" } has no callees; expected: "+annotation.toJava
            fail(message)
        }

        val calleeMatchingAnnotation =
            callees filter { callee ⇒
                val (pc, calledMethod) = callee
                val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(pc)

                calledMethod.name == methodName &&
                    (calledMethod.classFile.thisType eq receiver) &&
                    line == lineNumber
            }

        val unresolvedReceiverCalleesWithMatchingAnnotation =
            unresolvedMethodCalls filter { call ⇒
                val Some(line) = method.body.get.lineNumber(call.pc)

                receiverClassIsUnknown && // Just for performance
                    call.caller == method &&
                    call.calleeName == methodName &&
                    (call.calleeClass eq receiver) &&
                    line == lineNumber
            }

        val failCriteria =
            calleeMatchingAnnotation.isEmpty &&
                (!receiverClassIsUnknown || unresolvedReceiverCalleesWithMatchingAnnotation.isEmpty) &&
                preciselyResolvable

        if (algorithmSpecified && !preciselyResolvable && calleeMatchingAnnotation.nonEmpty) {
            val className = method.classFile.fqn
            val message = className+" { "+method+" } has more than the specified callees; "+
                "This edge should not exist using "+testCallGraphAlgorithm+
                "\nexpected: "+annotation.toJava+
                "\n actual: "+callees.map { callee ⇒
                    val (pc, method) = callee
                    (method.body.get.lineNumber(pc), method, method.classFile.thisType)
                }
            fail(message)
        }

        if (algorithmSpecified && preciselyResolvable && failCriteria) {
            val className = method.classFile.fqn
            val message = className+" { "+method+" } has more than the specified callees; expected: "+annotation.toJava+
                "\n actual: "+callees.map { callee ⇒
                    val (pc, method) = callee
                    (method.body.get.lineNumber(pc), method, method.classFile.thisType)
                }
            fail(message)
        }

        if (failCriteria) {
            val className = method.classFile.fqn
            val message =
                className+" { "+method+" } has none of the specified callees; expected: "+annotation.toJava+
                    "\n actual: "+callees.map { callee ⇒
                        val (pc, method) = callee
                        (method.body.get.lineNumber(pc), method, method.classFile.thisType)
                    }
            fail(message)
        }

        calleeMatchingAnnotation foreach { callee ⇒
            val (_, method) = callee
            method.name should be(methodName)
            method.classFile.thisType should be(receiver)
        }
    }

    // Single Constructor Test
    def singleConstructorTest(method: Method, annotation: Annotation): Unit = {

        // RETHINK

        val evps = annotation.elementValuePairs
        val Some(receiver) =
            evps collectFirst {
                case ElementValuePair("receiverType", StringValue(receiver)) ⇒ ObjectType(receiver)
            }

        val Some(lineNumber) =
            evps collectFirst {
                case ElementValuePair("line", IntValue(lineNumber)) ⇒ lineNumber
            }

        val isReflective: Boolean = {
            evps.collectFirst {
                case ElementValuePair("isReflective", BooleanValue(isReflective)) ⇒ isReflective
            }.getOrElse(false)
        }

        val receiverClassIsUnknown = project.classFile(receiver).isEmpty

        // If we are not able to handle reflective calls and we have one, forget about it
        if (isReflective && ignoreReflectiveCalls)
            cancel("ignoring reflection based test")

        val callees =
            callGraph.calls(method).flatMap { calledMethods ⇒
                val (pc, callees) = calledMethods
                callees map { ((pc, _)) }
            }

        if (callees.isEmpty) {
            val className = method.classFile.fqn
            val message = className+" { "+method+" } has no called constructors; expected: "+annotation.toJava
            fail(message)
        }

        val calleeMatchingAnnotation =
            callees filter { callee ⇒
                val (pc, calledMethod) = callee
                val Some(line) = method.body.get.lineNumberTable.get.lookupLineNumber(pc)
                calledMethod.name.equals("<init>") &&
                    calledMethod.classFile.thisType.equals(receiver) &&
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

        if (calleeMatchingAnnotation.isEmpty && (!receiverClassIsUnknown || unresolvedReceiverCalleesWithMatchingAnnotation.isEmpty)) {
            val className = method.classFile.fqn
            val message = className+" { "+method+" } has none of the specified constructor calls; expected: "+annotation.toJava+
                "\n actual: "+callees.map(f ⇒ (method.body.get.lineNumber(f._1), f._2, f._2.classFile.thisType))
            fail(message)
        }

        calleeMatchingAnnotation foreach { callee ⇒
            val (_, calledMethod) = callee
            calledMethod.name should be("<init>")
            calledMethod.classFile.thisType should be(receiver)
        }
    }

    // Single Field Access Test

    def singleFieldAccessTest(method: Method, annotation: Annotation): Unit = {
        /*TODO evaluate the result!
        val evps = annotation.elementValuePairs
        val (fqnClass) =
            evps collectFirst {
                case ElementValuePair("declaringType", ClassValue(declaringType)) ⇒
                    declaringType
            }

        val Some(fieldType) =
            evps collectFirst {
                case ElementValuePair("fieldType", ClassValue(fieldType)) ⇒ fieldType
            }

        val Some(fieldName) =
            evps collectFirst { case ElementValuePair("name", StringValue(name)) ⇒ name }

        val Some(lineNumber) =
            evps collectFirst {
                case ElementValuePair("line", IntValue(lineNumber)) ⇒ lineNumber
            }
*/

    }

    //
    // TESTS
    //

    "the computation of a call graph" should "should not throw any exceptions" in {
        if (exceptions.nonEmpty) {
            exceptions.foreach { ex ⇒ Console.err.println(ex.toString) }
            fail(s"call graph construction failed:")
        }
    }

    // Validate every method against the callgraph defined by annotations
    for {
        classFile ← project.allClassFiles
        method ← classFile.methods
    } {
        it should ("correctly identify all call targets for "+method.toJava) in {

            // single invocation per method
            method.runtimeVisibleAnnotations filter { annotation ⇒
                annotation.annotationType == invokedMethodAnnotation
            } foreach { invokedMethod ⇒
                singleMethodTest(method, invokedMethod)
            }

            // multiple invocations per Method
            method.runtimeVisibleAnnotations filter { annotation ⇒
                annotation.annotationType == invokedMethodsAnnotation
            } foreach { invokedMethodsAnnotation ⇒
                val Some(annotationArray) =
                    invokedMethodsAnnotation.elementValuePairs collectFirst {
                        case ElementValuePair("value", ArrayValue(array)) ⇒ array
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
            method.runtimeVisibleAnnotations filter {
                _.annotationType equals (invokedConstructorsAnnotation)
            } foreach { invokedConstructorsAnnotation ⇒
                val Some(annotationArray) =
                    invokedConstructorsAnnotation.elementValuePairs collectFirst {
                        case ElementValuePair("value", ArrayValue(array)) ⇒ array
                    }
                val annotations =
                    annotationArray collect {
                        case AnnotationValue(annotation) ⇒ annotation
                    }
                annotations foreach (singleConstructorTest(method, _))
            }

            // single field access per method
            method.runtimeVisibleAnnotations filter {
                _.annotationType equals (accessedFieldAnnotation)
            } foreach (singleFieldAccessTest(method, _))

            // multiple field accesses per method
            method.runtimeVisibleAnnotations filter {
                _.annotationType equals (accessedFieldsAnnotation)
            } foreach { accessedFieldsAnnotation ⇒
                val Some(annotationArray) =
                    accessedFieldsAnnotation.elementValuePairs collectFirst {
                        case ElementValuePair("value", ArrayValue(array)) ⇒ array
                    }
                val annotations = annotationArray collect {
                    case AnnotationValue(annotation) ⇒ annotation
                }
                annotations foreach (singleFieldAccessTest(method, _))
            }
        }
    }
}
