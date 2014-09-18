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

import org.junit.runner.RunWith

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

/**
 * Tests the methods that test the relation between numeric values.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class TypeRelationTest extends FunSpec with Matchers {

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

            Seq(ShortType, IntegerType, LongType, FloatType, DoubleType) foreach { t ⇒
                assert(!ByteType.isWiderThan(t))
                assert(!CharType.isWiderThan(t))
            }

            Seq(IntegerType, LongType, FloatType, DoubleType) foreach (t ⇒
                assert(!ShortType.isWiderThan(t)))

            Seq(LongType, FloatType, DoubleType) foreach (t ⇒
                assert(!IntegerType.isWiderThan(t)))

            Seq(FloatType, DoubleType) foreach (t ⇒ assert(!LongType.isWiderThan(t)))

            assert(!FloatType.isWiderThan(DoubleType))
        }
    }

}