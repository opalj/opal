/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.util.control.ControlThrowable
import org.opalj.bi.TestResources.locateTestResources

import scala.collection.immutable.ArraySeq

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class ClassFileTest extends AnyFunSuite with Matchers {

    import reader.Java8Framework.ClassFile

    lazy val codeJARFile = locateTestResources("code.jar", "bi")
    lazy val immutableList = ClassFile(codeJARFile, "code/ImmutableList.class").head
    lazy val boundedBuffer = ClassFile(codeJARFile, "code/BoundedBuffer.class").head
    lazy val quicksort = ClassFile(codeJARFile, "code/Quicksort.class").head

    test("test that it can find the first constructor") {
        assert(
            immutableList.findMethod("<init>", MethodDescriptor(ObjectType.Object, VoidType)).isDefined
        )
    }

    test("test that it can find the second constructor") {
        assert(
            immutableList.findMethod(
                "<init>",
                MethodDescriptor(
                    ArraySeq(ObjectType.Object, ObjectType("code/ImmutableList")), VoidType
                )
            ).isDefined
        )
    }

    test("test that all constructors are returned") {
        assert(immutableList.constructors.size.toInt == 2)
    }

    test("test that it can find all other methods") {
        assert(
            immutableList.findMethod(
                "getNext", MethodDescriptor(NoFieldTypes, ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "prepend", MethodDescriptor(ObjectType.Object, ObjectType("code/ImmutableList"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "getIterator", MethodDescriptor(NoFieldTypes, ObjectType("java/util/Iterator"))
            ).isDefined
        )

        assert(
            immutableList.findMethod(
                "get", MethodDescriptor(NoFieldTypes, ObjectType.Object)
            ).isDefined
        )

        assert(immutableList.instanceMethods.size == 4)
    }

    test("that findField on a class without fields does not fail") {
        quicksort.fields should be(empty)
        quicksort.findField("DoesNotExist") should be(List.empty)
    }

    test("that findField finds all fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("buffer") should not be (Symbol("Empty"))
        boundedBuffer.findField("first") should not be (Symbol("Empty"))
        boundedBuffer.findField("last") should not be (Symbol("Empty"))
        boundedBuffer.findField("size") should not be (Symbol("Empty"))
        boundedBuffer.findField("numberInBuffer") should not be (Symbol("Empty"))
    }

    test("that findField does not find non-existing fields") {
        if (boundedBuffer.fields.size != 5)
            fail("expected five fields; found: "+boundedBuffer.fields)

        boundedBuffer.findField("BUFFER") should be(List.empty)
        boundedBuffer.findField("firsT") should be(List.empty)
        boundedBuffer.findField("lAst") should be(List.empty)
        boundedBuffer.findField("Size") should be(List.empty)
        boundedBuffer.findField("AnumberInBuffers") should be(List.empty)
    }

    lazy val innerclassesProject = TestSupport.biProject("innerclasses-1.8-g-parameters-genericsignature.jar")
    lazy val outerClass = innerclassesProject.classFile(ObjectType("innerclasses/MyRootClass")).get
    lazy val innerPrinterOfXClass = innerclassesProject.classFile(ObjectType("innerclasses/MyRootClass$InnerPrinterOfX")).get
    lazy val formatterClass = innerclassesProject.classFile(ObjectType("innerclasses/MyRootClass$Formatter")).get

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
        outerClass.foreachNestedClass({ nc => foundNestedTypes += nc.thisType })(innerclassesProject)

        foundNestedTypes.size should be(expectedNestedTypes.size)
        foundNestedTypes should be(expectedNestedTypes)
    }

    private def testJARFile(jarFile: java.io.File) = {
        val project = analyses.Project(jarFile)
        var innerClassesCount = 0
        var failures: List[String] = List.empty
        val nestedTypes = for (classFile <- project.allClassFiles) yield {
            try {
                // should not time out or crash...
                classFile.nestedClasses(project)
                var nestedClasses: List[Type] = Nil
                classFile.foreachNestedClass({ c =>
                    nestedClasses = c.thisType :: nestedClasses
                    innerClassesCount += 1
                })(project)
                innerClassesCount += 1
                Some((classFile.thisType, nestedClasses))
            } catch {
                case ct: ControlThrowable => throw ct
                case t: Throwable =>
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
        nestedTypeInformation(o$1) should be(Symbol("Empty")) // the jar contains inconsistent code...
        nestedTypeInformation(o$1$q) should be(Symbol("Empty"))
    }

    test("that it is possible to get the inner classes information for batik-DOMViewer 1.7.jar") {
        val resources = locateTestResources("classfiles/batik-DOMViewer 1.7.jar", "bi")
        val nestedTypeInformation = testJARFile(resources)
        val D$Panel = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$Panel")
        val D$2 = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$2")
        val D$3 = ObjectType("org/apache/batik/apps/svgbrowser/DOMViewer$3")
        nestedTypeInformation(D$Panel) should contain(D$2)
        nestedTypeInformation(D$2) should contain(D$3)
        nestedTypeInformation(D$3) should be(Symbol("Empty"))
    }

    test("that it is possible to get the inner classes information for Apache ANT 1.8.4 - excerpt.jar") {
        testJARFile(locateTestResources("classfiles/Apache ANT 1.8.4 - excerpt.jar", "bi"))
    }

    test("that it is possible to get the inner classes information for argouml-excerpt.jar") {
        testJARFile(locateTestResources("classfiles/argouml-excerpt.jar", "bi"))
    }

}
