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

import org.opalj.br.instructions.IADD
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.IRETURN
import org.scalatest.FunSuite
import org.scalatest.Matchers

/**
 * Tests the configuration of the similarity test.
 *
 * The default functionality is also tested in ClassFileTest.
 *
 * @author Timothy Earley
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SimilarityTestConfigurationTest extends FunSuite with Matchers {

    //
    // Test Fixtures
    // (The following classes do NOT represent valid class files!)
    //
    def simpleAttributes = Vector(SourceFile("abc"), Deprecated)
    def simpleField = Field(1, "test", ByteType, simpleAttributes)
    def simpleFields = Vector(simpleField, Field(2, "field 2", BooleanType, simpleAttributes))
    def simpleCode = Code(2, 0, Array(ICONST_1, ICONST_1, IADD, IRETURN))
    def simpleMethod = Method(
        1,
        "simple_method",
        MethodDescriptor.JustReturnsBoolean,
        Vector(
            simpleCode,
            Deprecated
        )
    )
    def simpleMethod2 = Method(
        2,
        "simple_method_2",
        MethodDescriptor.NoArgsAndReturnVoid,
        Vector(Code(0, 0, Array()))
    )
    def simpleMethods = Vector(simpleMethod, simpleMethod2)
    def simpleClass = ClassFile(
        minorVersion = 1,
        majorVersion = 2,
        accessFlags = 3,
        thisType = ObjectType.Boolean,
        superclassType = Some(ObjectType.Object),
        interfaceTypes = List(ObjectType.Byte, ObjectType.Float),
        fields = simpleFields,
        methods = simpleMethods,
        attributes = simpleAttributes
    )

    //
    // TESTS
    //

    test("two identical class files are similar when all elements are compared") {
        assert(simpleClass.findDissimilarity(simpleClass, CompareAllConfiguration).isEmpty)
    }

    test("two identical class files are similar when only hard coded comparisons are done") {
        object NoTestsConfiguration extends SimilarityTestConfiguration {

            def compareFields(
                leftContext: ClassFile,
                left:        Fields,
                right:       Fields
            ): (Fields, Fields) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareMethods(
                leftContext: ClassFile,
                left:        Methods,
                right:       Methods
            ): (Methods, Methods) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                (IndexedSeq.empty, IndexedSeq.empty)
            }

            def compareCode(
                leftContext: Method,
                left:        Option[Code],
                right:       Option[Code]
            ): (Option[Code], Option[Code]) = {
                (None, None)
            }
        }
        assert(simpleClass.findDissimilarity(simpleClass, NoTestsConfiguration).isEmpty)
    }

    test("tests the comparison of two identical class files if only one attribute is compared") {
        object NotDeprecatedFilter extends CompareAllConfiguration {
            override def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                val (newLeft, newRight) = super.compareAttributes(leftContext, left, right)
                (
                    newLeft.filter(a ⇒ a.isInstanceOf[SourceFile]),
                    newRight.filter(a ⇒ a.isInstanceOf[SourceFile])
                )
            }
        }
        // the following class has less attributes
        val noLongerDeprecatedClass = simpleClass.copy(attributes = Vector(SourceFile("abc")))

        // normal test fails
        assert(!simpleClass.similar(noLongerDeprecatedClass))

        // ignoring certain attributes works both ways
        assert(
            simpleClass.similar(noLongerDeprecatedClass, NotDeprecatedFilter),
            simpleClass.findDissimilarity(noLongerDeprecatedClass, NotDeprecatedFilter).toString
        )
        assert(
            noLongerDeprecatedClass.similar(simpleClass, NotDeprecatedFilter)
        )
    }

    test("tests the comparison of two identical class files if only one attribute is selected") {
        object NotDeprecatedFilter extends CompareAllConfiguration {
            override def compareAttributes(
                leftContext: CommonAttributes,
                left:        Attributes,
                right:       Attributes
            ): (Attributes, Attributes) = {
                val (superNewLeft, superNewRight) = super.compareAttributes(leftContext, left, right)
                val (newLeft, newRight) = (
                    superNewLeft.filterNot(a ⇒ a.isInstanceOf[Deprecated]),
                    superNewRight.filterNot(a ⇒ a.isInstanceOf[Deprecated])
                )
                println(newLeft+" vs "+newRight)
                (newLeft, newRight)
            }
        }
        // the following class has less attributes
        val noLongerDeprecatedClass = simpleClass.copy(attributes = Vector(SourceFile("abc")))

        // normal test fails
        assert(!simpleClass.similar(noLongerDeprecatedClass))

        // ignoring certain attributes works both ways
        assert(
            simpleClass.similar(noLongerDeprecatedClass, NotDeprecatedFilter),
            simpleClass.findDissimilarity(noLongerDeprecatedClass, NotDeprecatedFilter).toString
        )
        assert(
            noLongerDeprecatedClass.similar(simpleClass, NotDeprecatedFilter)
        )
    }

    test("tests the comparison of two identical class files if some field is not defined") {
        object FieldsWithAccessFlagsEquals1 extends CompareAllConfiguration {
            override def compareFields(
                leftContext: ClassFile,
                left:        Fields,
                right:       Fields
            ): (Fields, Fields) = {
                (
                    left.filter(a ⇒ a.accessFlags == 1),
                    right.filter(a ⇒ a.accessFlags == 1)
                )
            }
        }
        // the following class has less fields
        val classWithLessFields = simpleClass.copy(fields = Vector(simpleField))

        // normal test fails
        assert(!simpleClass.similar(classWithLessFields))

        // ignoring certain fields works both ways
        assert(
            simpleClass.similar(classWithLessFields, FieldsWithAccessFlagsEquals1),
            simpleClass.attributes+" vs. "+classWithLessFields.attributes
        )
        assert(
            classWithLessFields.similar(simpleClass, FieldsWithAccessFlagsEquals1)
        )
    }

    /*
    test("test missing fields") {
        val classMissingFields = simpleClass.copy(fields = Vector(simpleField))
        val fieldsFilter = CompareAllConfiguration {
            override def testFieldsSize(fields: Fields): Boolean = false
            // only choose the first field
            override def testField(field: Field): Boolean = field.accessFlags == 1
        }

        assert(!simpleClass.similar(classMissingFields))
        assert(simpleClass.similar(classMissingFields, fieldsFilter))
        assert(classMissingFields.similar(simpleClass, fieldsFilter))
    }

    test("test missing method") {
        val methodFilter = new CompareAllConfiguration {
            override def testMethodsSize(methods: Methods): Boolean = false
            override def testMethod(method: Method): Boolean = false
        }

        val classMissingMethod = simpleClass.copy(methods = Vector(simpleMethod))

        assert(!simpleClass.similar(classMissingMethod))
        assert(simpleClass.similar(classMissingMethod, methodFilter))
        assert(classMissingMethod.similar(simpleClass, methodFilter))
    }

*/

}
