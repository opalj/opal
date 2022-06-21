/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funsuite.AnyFunSuite

/**
 * Tests that `ArrayType`s are represented as specified.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class ArrayTypeTest extends AnyFunSuite {

    test("ArrayType (Array of References) Field Descriptor") {
        val fieldType = FieldType("[Ljava/lang/Object;")
        val ArrayType(componentType) = fieldType
        assert(componentType === FieldType("Ljava/lang/Object;"))
    }

    test("ArrayType (Array of Primitives) Field Descriptor") {
        val fieldType = FieldType("[J")
        val ArrayType(componentType) = fieldType
        assert(componentType === LongType)
    }

    test("ArrayType (Array of Array of Primitives) Field Descriptor") {
        FieldType("[[S") match {
            case ArrayType(ArrayType(ShortType)) => /*OK*/
            case _type                           => throw new MatchError(_type)
        }
    }

    test("toJavaClass") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at1Class = at1.toJavaClass
        assert(at1Class == classOf[Array[Object]])

        val at2 = ArrayType(ObjectType("java/util/List"))
        assert(at2.toJavaClass == classOf[Array[java.util.List[_]]])

        val at3 = ArrayType(IntegerType)
        assert(at3.toJavaClass == classOf[Array[Int]])
    }

    test("Equality") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at2 = FieldType("[Ljava/lang/Object;")
        val at3: ArrayType = FieldType("[[Ljava/lang/Object;").asInstanceOf[ArrayType]

        assert(at1 == at2)
        assert(at3.componentType == at2)
    }

    test("Reference equality") {
        val at1 = FieldType("[Ljava/lang/Object;")
        val at2 = FieldType("[Ljava/lang/Object;")
        val at3 = FieldType("[[Ljava/lang/Object;").asInstanceOf[ArrayType]
        val at4 = FieldType("[Ljava/lang/String;").asInstanceOf[ArrayType]

        assert(at1 eq at2)
        assert(at3.componentType eq at2)
        assert(at4.componentType ne at2)
    }

    test("Pattern matching") {
        val at1: FieldType = FieldType("[[[Ljava/lang/Object;")

        at1 match {
            case ArrayType(ArrayType(ArrayType(ObjectType(className)))) =>
                assert(className === "java/lang/Object")
            case _type => throw new MatchError(_type)
        }
    }
}
