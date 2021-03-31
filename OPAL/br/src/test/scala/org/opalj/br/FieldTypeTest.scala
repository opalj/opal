/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funsuite.AnyFunSuite

/**
 * Test the construction and analysis of types.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatestplus.junit.JUnitRunner])
class FieldTypeTest extends AnyFunSuite {

    test("Byte Field Descriptor") {
        val fieldType = FieldType("B")
        assert(fieldType.toJavaClass == java.lang.Byte.TYPE)
        assert(fieldType == ByteType)
    }

    test("Char Field Descriptor") {
        val fieldType = FieldType("C")
        assert(fieldType.toJavaClass == java.lang.Character.TYPE)
        assert(fieldType == CharType)
    }

    test("Double Field Descriptor") {
        val fieldType = FieldType("D")
        assert(fieldType.toJavaClass == java.lang.Double.TYPE)
        assert(fieldType == DoubleType)
    }

    test("Float Field Descriptor") {
        val fieldType = FieldType("F")
        assert(fieldType.toJavaClass == java.lang.Float.TYPE)
        assert(fieldType == FloatType)
    }

    test("Integer Field Descriptor") {
        val fieldType = FieldType("I")
        assert(fieldType.toJavaClass == java.lang.Integer.TYPE)
        assert(fieldType == IntegerType)
    }

    test("Long Field Descriptor") {
        val fieldType = FieldType("J")
        assert(fieldType.toJavaClass == java.lang.Long.TYPE)
        assert(fieldType == LongType)
    }

}
