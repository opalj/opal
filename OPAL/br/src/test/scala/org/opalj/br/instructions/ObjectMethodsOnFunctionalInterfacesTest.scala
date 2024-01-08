/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import scala.collection.immutable.ArraySeq

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.reader.Java8Framework
import org.opalj.br.reader.Java8LibraryFramework

/**
 * Tests that calls to inherited methods on lambda instances go to Object.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ObjectMethodsOnFunctionalInterfacesTest extends AnyFunSpec with Matchers {

    val InvokedMethod = ObjectType("annotations/target/InvokedMethod")

    val project: SomeProject = {
        val testResources = locateTestResources("lambdas-1.8-g-parameters-genericsignature", "bi")
        val projectClasses = Java8Framework.ClassFiles(testResources)
        val libraryClasses = Java8LibraryFramework.ClassFiles(org.opalj.bytecode.RTJar)
        Project(projectClasses, libraryClasses, true)
    }

    private def testMethod(classFile: ClassFile, name: String): Unit = {
        for {
            method <- classFile.findMethod(name)
            body <- method.body
            invokevirtual <- body.instructions.view.collect { case i: INVOKEVIRTUAL => i }
            annotations = method.runtimeVisibleAnnotations
        } {
            val invokedMethod = getInvokedMethod(annotations)
            if (invokedMethod.isEmpty)
                throw new IllegalArgumentException(
                    s"the method which is expected to be invoked $annotations is not found"
                )

            val methodIdentifier = invokedMethod.get.signatureToJava(false)
            it(s"«$methodIdentifier» should resolve to Object's method") {
                val declaringClass = invokevirtual.declaringClass
                declaringClass should be(Symbol("ObjectType"))
                val declaringClassFile = project.classFile(declaringClass.asObjectType)
                declaringClassFile shouldBe defined
                val actualName = invokevirtual.name
                val actualDescriptor = invokevirtual.methodDescriptor
                val actualMethod = declaringClassFile.flatMap(
                    _.findMethod(actualName, actualDescriptor)
                )
                actualMethod shouldBe defined
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
        val candidates: ArraySeq[Option[Method]] = for {
            invokedMethod <- annotations.filter(_.annotationType == InvokedMethod)
            pairs = invokedMethod.elementValuePairs
            ElementValuePair("receiverType", StringValue(receiverType)) <- pairs
            ElementValuePair("name", StringValue(methodName)) <- pairs
            classFile <- project.classFile(ObjectType(receiverType))
        } yield {
            val parameterTypes = getParameterTypes(pairs)
            val returnType = getReturnType(pairs)
            val descriptor = MethodDescriptor(parameterTypes, returnType)
            classFile.findMethod(methodName, descriptor)
        }
        if (candidates.nonEmpty) candidates.head else None
    }

    private def getParameterTypes(pairs: ElementValuePairs): FieldTypes = {
        pairs.find(_.name == "parameterTypes").map { p =>
            p.value.asInstanceOf[ArrayValue].values.map[FieldType] {
                case ClassValue(x: ObjectType) => x
                case ClassValue(x: BaseType)   => x
                case x: ElementValue           => x.valueType
            }
        }.getOrElse(NoFieldTypes)
    }

    private def getReturnType(pairs: ElementValuePairs): Type = {
        pairs.find(_.name == "returnType").map { p =>
            p.value.asInstanceOf[ClassValue].value
        }.getOrElse(VoidType)
    }

    describe("invocations of inherited methods on instances of functional interfaces") {
        val testClassType = ObjectType("lambdas/ObjectMethodsOnFunctionalInterfaces")
        val testClass = project.classFile(testClassType).get
        val annotatedMethods = testClass.methods.filter(_.runtimeVisibleAnnotations.nonEmpty)
        annotatedMethods.foreach(m => testMethod(testClass, m.name))
    }
}
