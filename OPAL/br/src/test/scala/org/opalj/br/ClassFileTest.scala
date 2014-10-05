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

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.opalj.bi.TestSupport.locateTestResources
import scala.util.control.ControlThrowable
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time.Millis
import org.scalatest.time.Span

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ClassFileTest extends FunSuite with Matchers with ParallelTestExecution {

    import reader.Java8Framework.ClassFile

    val codeJARFile = locateTestResources("classfiles/Code.jar", "bi")
    val immutableList = ClassFile(codeJARFile, "code/ImmutableList.class").head
    val boundedBuffer = ClassFile(codeJARFile, "code/BoundedBuffer.class").head
    val quicksort = ClassFile(codeJARFile, "code/Quicksort.class").head

    test("test that it can find the first constructor") {
        assert(
            immutableList.findMethod(
                "<init>",
                MethodDescriptor(ObjectType.Object, VoidType)
            ).isDefined
        )
    }

    test("test that it can find the second constructor") {
        assert(
            immutableList.findMethod(
                "<init>",
                MethodDescriptor(
                    IndexedSeq(ObjectType.Object, ObjectType("code/ImmutableList")),
                    VoidType)
            ).isDefined
        )
    }

    test("test that all constructors are returned") {
        assert(
            immutableList.constructors.size == 2
        )
    }

    test("test that it can find all other methods") {
        assert(
            immutableList.findMethod(
                "getNext",
                MethodDescriptor(IndexedSeq(), ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "prepend",
                MethodDescriptor(ObjectType.Object, ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "getIterator",
                MethodDescriptor(IndexedSeq(), ObjectType("java/util/Iterator"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "get",
                MethodDescriptor(IndexedSeq(), ObjectType.Object)
            ).isDefined
        )

        assert(immutableList.instanceMethods.size == 4)
    }

    test("that findField on a class without fields does not fail") {
        quicksort.fields should be(empty)
        quicksort.findField("DoesNotExist") should be(None)
    }

    test("that findField finds all fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("buffer") should be('defined)
        boundedBuffer.findField("first") should be('defined)
        boundedBuffer.findField("last") should be('defined)
        boundedBuffer.findField("size") should be('defined)
        boundedBuffer.findField("numberInBuffer") should be('defined)
    }

    test("that findField does not find non-existing fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("BUFFER") should be(None)
        boundedBuffer.findField("firsT") should be(None)
        boundedBuffer.findField("lAst") should be(None)
        boundedBuffer.findField("Size") should be(None)
        boundedBuffer.findField("AnumberInBuffers") should be(None)
    }

    val innerclassesJARFile = locateTestResources("classfiles/Innerclasses.jar", "bi")
    val innerclassesProject = analyses.Project(innerclassesJARFile)
    val outerClass = ClassFile(innerclassesJARFile, "innerclasses/MyRootClass.class").head
    val innerPrinterOfXClass = ClassFile(innerclassesJARFile, "innerclasses/MyRootClass$InnerPrinterOfX.class").head
    val formatterClass = ClassFile(innerclassesJARFile, "innerclasses/MyRootClass$Formatter.class").head

    test("that all direct nested classes of a top-level class are correctly identified") {
        outerClass.nestedClasses(innerclassesProject).toSet should be(Set(
            ObjectType("innerclasses/MyRootClass$1"),
            ObjectType("innerclasses/MyRootClass$1MyInnerPrinter"),
            ObjectType("innerclasses/MyRootClass$2"),
            ObjectType("innerclasses/MyRootClass$Formatter"),
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX")
        ))
    }

    test("that all direct nested classes of a member class are correctly identified") {
        innerPrinterOfXClass.nestedClasses(innerclassesProject).toSet should be(Set(
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX$1"),
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX$InnerPrettyPrinter")
        ))
    }

    test("that no supertype information is extracted") {
        formatterClass.nestedClasses(innerclassesProject).toSet should be(Set.empty)
    }

    test("that all direct and indirect nested classes of a top-level class are correctly identified") {
        val expectedNestedTypes = Set(
            ObjectType("innerclasses/MyRootClass$2"),
            ObjectType("innerclasses/MyRootClass$Formatter"),
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX"),
            ObjectType("innerclasses/MyRootClass$1"),
            ObjectType("innerclasses/MyRootClass$1MyInnerPrinter"),
            ObjectType("innerclasses/MyRootClass$1$InnerPrinterOfAnonymousClass"),
            ObjectType("innerclasses/MyRootClass$1$1"),
            ObjectType("innerclasses/MyRootClass$1$1$1"),
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX$InnerPrettyPrinter"),
            ObjectType("innerclasses/MyRootClass$InnerPrinterOfX$1")
        )

        var foundNestedTypes: Set[ObjectType] = Set.empty
        outerClass.foreachNestedClass(innerclassesProject, { nc ⇒ foundNestedTypes += nc.thisType })

        foundNestedTypes.size should be(expectedNestedTypes.size)
        foundNestedTypes should be(expectedNestedTypes)
    }

    private def testJARFile(jarFile: java.io.File) = {
        val project = analyses.Project(jarFile)
        var innerClassesCount = 0
        var failures: List[String] = List.empty
        val nestedTypes = for (classFile ← project.classFiles) yield {
            try {
                // should not time out or crash...
                classFile.nestedClasses(project)
                var nestedClasses: List[Type] = Nil
                classFile.foreachNestedClass(project, { c ⇒
                    nestedClasses = c.thisType :: nestedClasses
                    innerClassesCount += 1
                })
                innerClassesCount += 1
                Some((classFile.thisType, nestedClasses))
            } catch {
                case ct: ControlThrowable ⇒ throw ct
                case t: Throwable ⇒
                    failures =
                        s"cannot calculate inner classes for ${classFile.fqn}: ${t.getClass().getSimpleName()} - ${t.getMessage()}" ::
                            failures
                    None
            }
        }
        if (failures.nonEmpty) {
            fail(failures.mkString("; "))
        }

        innerClassesCount should be > (0)
        nestedTypes.flatten.toSeq.toMap
    }

    test("that it is possible to get the inner classes information for batik-AbstractJSVGComponent.jar") {
        val resources = locateTestResources("classfiles/batik-AbstractJSVGComponent.jar", "bi")
        val nestedTypeInformation = testJARFile(resources)
        val o = ObjectType("org/apache/batik/swing/svg/AbstractJSVGComponent")
        val o$1 = ObjectType("org/apache/batik/swing/svg/AbstractJSVGComponent$1")
        val o$1$q = ObjectType("org/apache/batik/swing/svg/AbstractJSVGComponent$1$Query")
        nestedTypeInformation(o) should contain(o$1)
        nestedTypeInformation(o$1) should be('empty) // the jar contains inconsistent code...
        nestedTypeInformation(o$1$q) should be('empty)
    }

    test("that it is possible to get the inner classes information for batik-DOMViewer 1.7.jar") {
        val resources = locateTestResources("classfiles/batik-DOMViewer 1.7.jar", "bi")
        val nestedTypeInformation = testJARFile(resources)
        val D$Panel = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$Panel")
        val D$2 = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$2")
        val D$3 = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$3")
        nestedTypeInformation(D$Panel) should contain(D$2)
        nestedTypeInformation(D$2) should contain(D$3)
        nestedTypeInformation(D$3) should be('empty)
    }

    test("that it is possible to get the inner classes information for Apache ANT 1.8.4 - excerpt.jar") {
        testJARFile(locateTestResources("classfiles/Apache ANT 1.8.4 - excerpt.jar", "bi"))
    }

    test("that it is possible to get the inner classes information for argouml-excerpt.jar") {
        testJARFile(locateTestResources("classfiles/argouml-excerpt.jar", "bi"))
    }

}
