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

package org.opalj.br

import org.opalj.br.instructions.IADD
import org.opalj.br.instructions.ICONST_1
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.Instruction
import org.scalatest.FunSuite
import org.scalatest.Matchers

/**
 *
 * Tests the effect of SimilarityTestConfig on the similarity test.
 *
 * The default functionality is also tested in ClassFileTest.
 *
 * @author Timothy Earley
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SimilarityTestConfigTest extends FunSuite with Matchers {

    // define a bunch of simple objects to perform the tests on

    val simpleAttributes = Vector(SourceFile("abc"), Deprecated)

    val simpleField = Field(1, "test", ByteType, simpleAttributes)
    val simpleFields = Vector(
        simpleField,
        Field(2, "field 2", BooleanType, simpleAttributes)
    )
    val simpleCode = Code(
        2,
        0,
        Array(
            ICONST_1,
            ICONST_1,
            IADD,
            IRETURN
        )
    )

    val simpleMethod = Method(
        1,
        "simple method",
        MethodDescriptor.JustReturnsBoolean,
        Vector(
            simpleCode,
            Deprecated
        )
    )

    val simpleMethod2 = Method(
        2,
        "simple method 2",
        MethodDescriptor.NoArgsAndReturnVoid,
        Vector(
            Code(0, 0, Array())
        )
    )

    val simpleMethods = Vector(
        simpleMethod,
        simpleMethod2

    )

    val simpleClass = ClassFile(
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

    test("test missing attributes") {

        val attributeFilter: SimilarityTestConfig = new DefaultSimilarityTestConfig {
            override def testAttributesSize(attributes: Attributes) = false
            override def testAttribute(attribute: Attribute): Boolean = attribute match {
                case _: SourceFile ⇒ true
                case _             ⇒ false
            }
        }
        val classMissingAttributes: ClassFile = simpleClass.copy(attributes = Vector(SourceFile("abc")))

        // normal test fails
        assert(!simpleClass.similar(classMissingAttributes))
        // filtering the attributes works both ways
        assert(simpleClass.similar(classMissingAttributes, attributeFilter))
        assert(classMissingAttributes.similar(simpleClass, attributeFilter))

    }

    test("wrong access flags") {

        val accesFlagFilter: SimilarityTestConfig = new DefaultSimilarityTestConfig {
            override def testAccessFlags(accessFlags: SourceElementID): Boolean = false
        }

        val classFileWrongAccessFlags = simpleClass.copy(accessFlags = -1)

        assert(!simpleClass.similar(classFileWrongAccessFlags))
        assert(simpleClass.similar(classFileWrongAccessFlags, accesFlagFilter))
        assert(classFileWrongAccessFlags.similar(simpleClass, accesFlagFilter))

        // same thing with fields
        val fieldWithWrongAccessFlags = simpleFields(0).copy(accessFlags = -1)
        assert(!simpleField.similar(fieldWithWrongAccessFlags))
        assert(simpleField.similar(fieldWithWrongAccessFlags, accesFlagFilter))
        assert(fieldWithWrongAccessFlags.similar(simpleField, accesFlagFilter))

        // ... and methods
        val methodWithWrongAccessFlags = simpleMethod.copy(accessFlags = -1)
        assert(!simpleMethod.similar(methodWithWrongAccessFlags))
        assert(simpleMethod.similar(methodWithWrongAccessFlags, accesFlagFilter))
        assert(methodWithWrongAccessFlags.similar(simpleMethod, accesFlagFilter))

    }

    test("test missing fields") {
        val classMissingFields = simpleClass.copy(fields = Vector(simpleField))
        val fieldsFilter = new DefaultSimilarityTestConfig {
            override def testFieldsSize(fields: Fields): Boolean = false
            // only choose the first field
            override def testField(field: Field): Boolean = field.accessFlags == 1
        }

        assert(!simpleClass.similar(classMissingFields))
        assert(simpleClass.similar(classMissingFields, fieldsFilter))
        assert(classMissingFields.similar(simpleClass, fieldsFilter))
    }

    test("test wrong types") {
        val thisTypeFilter = new DefaultSimilarityTestConfig {
            override def testType(thisType: ObjectType) = false
        }
        val interfaceTypesFilter = new DefaultSimilarityTestConfig {
            override def testInterfaceTypes(interfaceTypes: Seq[ObjectType]) = false
        }
        val superclassTypeFilter = new DefaultSimilarityTestConfig {
            override def testSuperclassType(superClassType: Option[ObjectType]) = false
        }

        val classWrithWrongThisType = simpleClass.copy(thisType = ObjectType.Error)
        val classWithWrongInterfacesType = simpleClass.copy(interfaceTypes = List())
        val classWithWrongSuperclassType = simpleClass.copy(
            superclassType = Some(ObjectType.Error)
        )

        assert(!simpleClass.similar(classWrithWrongThisType))
        assert(simpleClass.similar(classWrithWrongThisType, thisTypeFilter))
        assert(classWrithWrongThisType.similar(simpleClass, thisTypeFilter))

        assert(!simpleClass.similar(classWithWrongInterfacesType))
        assert(simpleClass.similar(classWithWrongInterfacesType, interfaceTypesFilter))
        assert(classWithWrongInterfacesType.similar(simpleClass, interfaceTypesFilter))

        assert(!simpleClass.similar(classWithWrongSuperclassType))
        assert(simpleClass.similar(classWithWrongSuperclassType, superclassTypeFilter))
        assert(classWithWrongSuperclassType.similar(simpleClass, superclassTypeFilter))
    }

    test("test missing method") {
        val methodFilter = new DefaultSimilarityTestConfig {
            override def testMethodsSize(methods: Methods): Boolean = false
            override def testMethod(method: Method): Boolean = false
        }

        val classMissingMethod = simpleClass.copy(methods = Vector(simpleMethod))

        assert(!simpleClass.similar(classMissingMethod))
        assert(simpleClass.similar(classMissingMethod, methodFilter))
        assert(classMissingMethod.similar(simpleClass, methodFilter))
    }

    test("test missing body") {
        val methodMissingBody = simpleMethod.copy(body = None)
        val bodyFilter = new DefaultSimilarityTestConfig {
            override def checkBody(body: Option[Code]): Boolean = false
        }

        assert(!simpleMethod.similar(methodMissingBody))
        assert(simpleMethod.similar(methodMissingBody, bodyFilter))
        assert(methodMissingBody.similar(simpleMethod, bodyFilter))
    }

    test("test wrong instruction") {
        val codeMissingInstruction = Code(
            2,
            0,
            Array(
                ICONST_1,
                ICONST_1,
                /* IADD, */
                IRETURN
            )
        )

        val instructionFilter = new DefaultSimilarityTestConfig {
            override def testInstructionsLength(instructions: Instructions) = false
            override def testInstruction(instruction: Instruction): Boolean = {
                instruction match {
                    case IADD ⇒ false
                    case _    ⇒ true
                }
            }
        }

        assert(!simpleCode.similar(codeMissingInstruction))
        assert(simpleCode.similar(codeMissingInstruction, instructionFilter))
        assert(codeMissingInstruction.similar(simpleCode, instructionFilter))

    }

}