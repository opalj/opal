/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funsuite.AnyFunSuite

/**
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class ObjectTypeTest extends AnyFunSuite {

    test("FieldType factory method") {
        val fieldType = FieldType("Ljava/lang/Object;")
        val ObjectType(className) = fieldType

        assert(className === "java/lang/Object")
    }

    test("toJavaClass method") {
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")
        val ot4 = ObjectType("java/util/List")

        assert(ot2.toJavaClass == classOf[Object])
        assert(ot3.toJavaClass == classOf[String])
        assert(ot4.toJavaClass == classOf[java.util.List[_]])
    }

    test("equals method") {
        val ot1 = ObjectType("java/lang/Object")
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")

        assert(ot1 == ot2)
        assert(ot1 != ot3)
    }

    test("primitiveTypeWrapperMatcher method") {

        val matcher = ObjectType.primitiveTypeWrapperMatcher[Int, (Int, Int)](
            (id) => (id, BooleanType.WrapperType.id),
            (id) => (id, ByteType.WrapperType.id),
            (id) => (id, CharType.WrapperType.id),
            (id) => (id, ShortType.WrapperType.id),
            (id) => (id, IntegerType.WrapperType.id),
            (id) => (id, LongType.WrapperType.id),
            (id) => (id, FloatType.WrapperType.id),
            (id) => (id, DoubleType.WrapperType.id),
            (id) => (-1, id)
        )
        assert(matcher(BooleanType.WrapperType, 1) == ((1, BooleanType.WrapperType.id)))
        assert(matcher(ByteType.WrapperType, 2) == ((2, ByteType.WrapperType.id)))
        assert(matcher(CharType.WrapperType, 3) == ((3, CharType.WrapperType.id)))
        assert(matcher(ShortType.WrapperType, 4) == ((4, ShortType.WrapperType.id)))
        assert(matcher(IntegerType.WrapperType, 5) == ((5, IntegerType.WrapperType.id)))
        assert(matcher(LongType.WrapperType, 6) == ((6, LongType.WrapperType.id)))
        assert(matcher(FloatType.WrapperType, 7) == ((7, FloatType.WrapperType.id)))
        assert(matcher(DoubleType.WrapperType, 8) == ((8, DoubleType.WrapperType.id)))
        assert(matcher(ObjectType.String, ObjectType.String.id) == ((-1, ObjectType.String.id)))

        assert(matcher(BooleanType.WrapperType, 10) == ((10, BooleanType.WrapperType.id)))
        assert(matcher(ByteType.WrapperType, 20) == ((20, ByteType.WrapperType.id)))
        assert(matcher(CharType.WrapperType, 30) == ((30, CharType.WrapperType.id)))
        assert(matcher(ShortType.WrapperType, 40) == ((40, ShortType.WrapperType.id)))
        assert(matcher(IntegerType.WrapperType, 50) == ((50, IntegerType.WrapperType.id)))
        assert(matcher(LongType.WrapperType, 60) == ((60, LongType.WrapperType.id)))
        assert(matcher(FloatType.WrapperType, 70) == ((70, FloatType.WrapperType.id)))
        assert(matcher(DoubleType.WrapperType, 80) == ((80, DoubleType.WrapperType.id)))
        assert(matcher(ObjectType.String, ObjectType.String.id) == ((-1, ObjectType.String.id)))
    }

    test("reference equality property") {
        val ot1 = ObjectType("java/lang/Object")
        val ot2 = ObjectType("java/lang/Object")
        val ot3 = ObjectType("java/lang/String")

        assert(ot1 eq ot2)
        assert(ot1 ne ot3)
    }

    test("pattern matching on ObjectTypes") {
        val ot1: FieldType = ObjectType("java/lang/Object")

        ot1 match {
            case ObjectType(c) => assert(c === "java/lang/Object")
            case _             => fail(s"pattern match on ObjectType ($ot1) failed")
        }
    }
}
