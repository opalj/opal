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

import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.opalj.br.instructions._

/**
 * Tests the factory methods for [[NumericConversionInstruction]]s.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class TypeConversionTest extends FunSpec with Matchers {

    describe("NumericType.convertTo") {

        describe("converting primitive types") {

            it("should return an empty array if source and target types are identical") {
                ByteType.convertTo(ByteType) should have size (0)
                CharType.convertTo(CharType) should have size (0)
                ShortType.convertTo(ShortType) should have size (0)
                IntegerType.convertTo(IntegerType) should have size (0)
                LongType.convertTo(LongType) should have size (0)
                FloatType.convertTo(FloatType) should have size (0)
                DoubleType.convertTo(DoubleType) should have size (0)
            }

            it("conversion of int like values to int values requires no code") {
                ByteType.convertTo(IntegerType) should have size (0)
                CharType.convertTo(IntegerType) should have size (0)
                ShortType.convertTo(IntegerType) should have size (0)
            }

            it("should be able to convert from int to other integer types") {
                IntegerType.convertTo(ShortType) should be(Array(I2S))
                IntegerType.convertTo(ByteType) should be(Array(I2B))
                IntegerType.convertTo(CharType) should be(Array(I2C))
                IntegerType.convertTo(LongType) should be(Array(I2L))
            }

            it("should be able to convert from int to floating point types") {
                IntegerType.convertTo(FloatType) should be(Array(I2F))
                IntegerType.convertTo(DoubleType) should be(Array(I2D))
            }

            it("should be able to convert from byte to other integer types") {
                ByteType.convertTo(ShortType) should have size (0)
                ByteType.convertTo(CharType) should be(Array(I2C))
                ByteType.convertTo(LongType) should be(Array(I2L))
            }

            it("should be able to convert from byte to floating point types") {
                ByteType.convertTo(FloatType) should be(Array(I2F))
                ByteType.convertTo(DoubleType) should be(Array(I2D))
            }

            it("should be able to convert from char to other integer types") {
                CharType.convertTo(ShortType) should be(Array(I2S))
                CharType.convertTo(ByteType) should be(Array(I2B))
                CharType.convertTo(LongType) should be(Array(I2L))
            }

            it("should be able to convert from char to floating point types") {
                CharType.convertTo(FloatType) should be(Array(I2F))
                CharType.convertTo(DoubleType) should be(Array(I2D))
            }

            it("should be able to convert from short to other integer types") {
                ShortType.convertTo(ByteType) should be(Array(I2B))
                ShortType.convertTo(CharType) should be(Array(I2C))
                ShortType.convertTo(LongType) should be(Array(I2L))
            }

            it("should be able to convert from short to floating point types") {
                ShortType.convertTo(FloatType) should be(Array(I2F))
                ShortType.convertTo(DoubleType) should be(Array(I2D))
            }

            it("should be able to convert from long to other integer types") {
                LongType.convertTo(IntegerType) should be(Array(L2I))
                LongType.convertTo(ByteType) should be(Array(L2I, I2B))
                LongType.convertTo(CharType) should be(Array(L2I, I2C))
                LongType.convertTo(ShortType) should be(Array(L2I, I2S))
            }

            it("should be able to convert from long to floating point types") {
                LongType.convertTo(FloatType) should be(Array(L2F))
                LongType.convertTo(DoubleType) should be(Array(L2D))
            }

            it("should be able to convert from float to double and back") {
                FloatType.convertTo(DoubleType) should be(Array(F2D))
                DoubleType.convertTo(FloatType) should be(Array(D2F))
            }

            it("should be able to convert from float to integer types") {
                FloatType.convertTo(LongType) should be(Array(F2L))
                FloatType.convertTo(IntegerType) should be(Array(F2I))
                FloatType.convertTo(ByteType) should be(Array(F2I, I2B))
                FloatType.convertTo(CharType) should be(Array(F2I, I2C))
                FloatType.convertTo(ShortType) should be(Array(F2I, I2S))
            }

            it("should be able to convert from double to integer types") {
                DoubleType.convertTo(LongType) should be(Array(D2L))
                DoubleType.convertTo(IntegerType) should be(Array(D2I))
                DoubleType.convertTo(ByteType) should be(Array(D2I, I2B))
                DoubleType.convertTo(CharType) should be(Array(D2I, I2C))
                DoubleType.convertTo(ShortType) should be(Array(D2I, I2S))
            }
        }

        describe("boxing primitive types") {
            BaseType.baseTypes foreach { t ⇒
                it(s"should convert ${t.toJava} to ${t.WrapperType.toJava}") {
                    val instructions = t.boxValue
                    instructions should be(Array(
                        INVOKESTATIC(t.WrapperType, "valueOf",
                            MethodDescriptor(t, t.WrapperType)),
                        null,
                        null
                    ))
                    t.adapt(ObjectType.Object) should be(t.boxValue)
                }
            }
        }

        describe("unboxing to primitive types") {
            BaseType.baseTypes foreach { t ⇒
                it(s"should convert ${t.WrapperType.toJava} to ${t.toJava}") {
                    val instructions = t.WrapperType.unboxValue
                    instructions should be(Array(
                        INVOKEVIRTUAL(t.WrapperType, s"${t.toJava}Value",
                            MethodDescriptor(IndexedSeq.empty, t)),
                        null,
                        null
                    ))
                }
            }
        }

    }
}