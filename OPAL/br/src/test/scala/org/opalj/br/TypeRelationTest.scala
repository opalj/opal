/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

/**
 * Tests the methods that test the relation between numeric values.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class TypeRelationTest extends AnyFunSpec with Matchers {

    describe("NumericType.isWiderThan") {

        it("should not be reflexive") {
            assert(!(ByteType isWiderThan ByteType))
            assert(!(CharType isWiderThan CharType))
            assert(!(ShortType isWiderThan ShortType))
            assert(!(IntegerType isWiderThan IntegerType))
            assert(!(LongType isWiderThan LongType))
            assert(!(FloatType isWiderThan FloatType))
            assert(!(DoubleType isWiderThan DoubleType))
        }

        it("should be true for \"wider\" types") {

            assert(ShortType isWiderThan ByteType)
            assert(IntegerType isWiderThan ByteType)
            assert(LongType isWiderThan ByteType)
            assert(FloatType isWiderThan ByteType)
            assert(DoubleType isWiderThan ByteType)

            assert(IntegerType isWiderThan CharType)
            assert(LongType isWiderThan CharType)
            assert(FloatType isWiderThan CharType)
            assert(DoubleType isWiderThan CharType)

            assert(IntegerType isWiderThan ShortType)
            assert(LongType isWiderThan ShortType)
            assert(FloatType isWiderThan ShortType)
            assert(DoubleType isWiderThan ShortType)

            assert(LongType isWiderThan IntegerType)
            assert(FloatType isWiderThan IntegerType)
            assert(DoubleType isWiderThan IntegerType)

            assert(FloatType isWiderThan LongType)
            assert(DoubleType isWiderThan LongType)

            assert(DoubleType isWiderThan FloatType)
        }

        it("should be false for types that are not larger") {

            Seq(ShortType, IntegerType, LongType, FloatType, DoubleType) foreach { t =>
                assert(!ByteType.isWiderThan(t))
                assert(!CharType.isWiderThan(t))
            }

            Seq(IntegerType, LongType, FloatType, DoubleType) foreach (t =>
                assert(!ShortType.isWiderThan(t)))

            Seq(LongType, FloatType, DoubleType) foreach (t =>
                assert(!IntegerType.isWiderThan(t)))

            Seq(FloatType, DoubleType) foreach (t => assert(!LongType.isWiderThan(t)))

            assert(!FloatType.isWiderThan(DoubleType))
        }
    }

}