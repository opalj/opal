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
 * Tests the factory method for [[NumericConversionInstruction]]s.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class TypeConversionTest extends FunSpec with Matchers {
    describe("Type.isWideningPrimitiveConversion") {
        it("should not be reflexive") {
            assert(!(BooleanType isWiderThan BooleanType))
            assert(!(ByteType isWiderThan ByteType))
            assert(!(CharType isWiderThan CharType))
            assert(!(ShortType isWiderThan ShortType))
            assert(!(IntegerType isWiderThan IntegerType))
            assert(!(LongType isWiderThan LongType))
            assert(!(FloatType isWiderThan FloatType))
            assert(!(DoubleType isWiderThan DoubleType))
        }
        it("should be true for \"smaller\" types") {
            assert(CharType isWiderThan BooleanType)
            assert(ByteType isWiderThan BooleanType)
            assert(ShortType isWiderThan BooleanType)
            assert(IntegerType isWiderThan BooleanType)
            assert(LongType isWiderThan BooleanType)
            assert(FloatType isWiderThan BooleanType)
            assert(DoubleType isWiderThan BooleanType)

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
        it("should be false for \"larger\" types") {
            BaseType.baseTypes foreach (t ⇒ assert(!BooleanType.isWiderThan(t)))
            Seq(ShortType, IntegerType, LongType, FloatType, DoubleType) foreach { t ⇒
                assert(!ByteType.isWiderThan(t))
                assert(!CharType.isWiderThan(t))
            }
            Seq(IntegerType, LongType, FloatType, DoubleType) foreach (t ⇒ assert(!ShortType.isWiderThan(t)))
            Seq(LongType, FloatType, DoubleType) foreach (t ⇒ assert(!IntegerType.isWiderThan(t)))
            Seq(FloatType, DoubleType) foreach (t ⇒ assert(!LongType.isWiderThan(t)))
            assert(!FloatType.isWiderThan(DoubleType))
        }
    }
    describe("Type.convertTo") {
        describe("converting primitive types") {
            it("should throw an exception if a boolean type is passed") {
                an[UnsupportedOperationException] should be thrownBy {
                    BooleanType.convertTo(IntegerType)
                }
                an[UnsupportedOperationException] should be thrownBy {
                    IntegerType.convertTo(BooleanType)
                }
                an[UnsupportedOperationException] should be thrownBy {
                    BooleanType.convertTo(BooleanType)
                }
            }
            it("should return an empty array if source and target types are identical") {
                ByteType.convertTo(ByteType) should have size (0)
                CharType.convertTo(CharType) should have size (0)
                DoubleType.convertTo(DoubleType) should have size (0)
                FloatType.convertTo(FloatType) should have size (0)
                IntegerType.convertTo(IntegerType) should have size (0)
                LongType.convertTo(LongType) should have size (0)
                ShortType.convertTo(ShortType) should have size (0)
            }
            it("should not be necessary to convert small integer types to int") {
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
                ByteType.convertTo(ShortType) should be(Array(I2S))
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
                    val instructions = t.convertTo(t.WrapperType)
                    instructions should be(Array(
                        INVOKESTATIC(t.WrapperType, "valueOf",
                            MethodDescriptor(t, t.WrapperType)),
                        null,
                        null
                    ))
                    t.convertTo(ObjectType.Object) should be(t.convertTo(t.WrapperType))
                    an[UnsupportedOperationException] should be thrownBy {
                        t.convertTo(ObjectType.Exception)
                    }
                }
            }
        }
        describe("unboxing to primitive types") {
            BaseType.baseTypes foreach { t ⇒
                it(s"should convert ${t.WrapperType.toJava} to ${t.toJava}") {
                    val instructions = t.WrapperType.convertTo(t)
                    instructions should be(Array(
                        INVOKEVIRTUAL(t.WrapperType, s"${t.toJava}Value",
                            MethodDescriptor(IndexedSeq.empty, t)),
                        null,
                        null
                    ))
                    t.WrapperType.convertTo(ObjectType.Object) should have size (0)
                    t.WrapperType.convertTo(ObjectType.Exception) should be(
                        Array(CHECKCAST(ObjectType.Exception), null, null))
                }
            }
        }
        describe("converting object types") {
            it("should simply cast any object types passed in") {
                val types = Seq(ObjectType.Object, ObjectType.Boolean, ObjectType.String,
                    ObjectType.Exception)
                for {
                    sourceType ← types
                    targetType ← types
                    if sourceType != targetType && targetType != ObjectType.Object
                } {
                    val conv = sourceType.convertTo(targetType)
                    conv should be(Array(CHECKCAST(targetType), null, null))
                }
            }
        }
        describe("converting array types") {
            it("no conversion should be necessary for identical array types") {
                val types = Seq(ArrayType.ArrayOfObjects, ArrayType(LongType),
                    ArrayType(ObjectType.String), ArrayType(DoubleType))
                types foreach { t ⇒
                    val conv = t.convertTo(t)
                    conv should have size (0)
                }
            }
            it("no conversion should be necessary to put an array into Object") {
                val types = Seq(ArrayType.ArrayOfObjects, ArrayType(LongType),
                    ArrayType(ObjectType.String), ArrayType(DoubleType))
                types foreach { t ⇒
                    val conv = t.convertTo(ObjectType.Object)
                    conv should have size (0)
                }
            }
            it("converting from or to array types of base types should not be possible") {
                val types = Seq(ArrayType(IntegerType), ArrayType(BooleanType),
                    ArrayType(LongType), ArrayType(FloatType))
                for {
                    typeA ← types
                    typeB ← types
                    if typeA != typeB
                } {
                    an[UnsupportedOperationException] should be thrownBy {
                        typeA.convertTo(typeB)
                    }
                }
            }
            it("converting arrays to non array types except Object should also not be possible") {
                val types = Seq(BooleanType, ObjectType.String, DoubleType)
                types foreach { t ⇒
                    an[UnsupportedOperationException] should be thrownBy {
                        ArrayType.ArrayOfObjects.convertTo(t)
                    }
                }
            }
            it("converting from reference array types to other reference array types should be a simple cast") {
                val types = Seq(ArrayType.ArrayOfObjects, ArrayType(ObjectType.Integer),
                    ArrayType(ObjectType.String), ArrayType(ObjectType.Exception))
                for {
                    typeA ← types
                    typeB ← types
                    if typeA != typeB
                } {
                    val conv = typeA.convertTo(typeB)
                    conv should be(Array(CHECKCAST(typeB), null, null))
                }
            }
            it("converting Object arrays with arbitrary dimensions to arrays of anything with higher dimensions should be a simple cast and vice versa") {
                for {
                    dimension ← (1 to 20)
                    componentType ← (BaseType.baseTypes ++ IndexedSeq(ObjectType.String, ObjectType.Integer))
                } {
                    val objectArray = ArrayType(dimension, ObjectType.Object)
                    val componentArray = ArrayType(dimension + 1, componentType)
                    objectArray.convertTo(componentArray) should be(Array(CHECKCAST(componentArray), null, null))
                    componentArray.convertTo(objectArray) should be(Array(CHECKCAST(objectArray), null, null))
                }
            }
            it("converting Object arrays of arbitrary dimensions to arrays of anything with lower dimensions should not be possible") {
                for {
                    dimension ← (1 to 20)
                    componentType ← (BaseType.baseTypes ++ IndexedSeq(ObjectType.String, ObjectType.Integer))
                } {
                    val objectArray = ArrayType(dimension + 1, ObjectType.Object)
                    val componentArray = ArrayType(dimension, componentType)
                    an[UnsupportedOperationException] should be thrownBy {
                        objectArray.convertTo(componentArray)
                    }
                    an[UnsupportedOperationException] should be thrownBy {
                        componentArray.convertTo(objectArray)
                    }
                }
            }
        }
    }
}