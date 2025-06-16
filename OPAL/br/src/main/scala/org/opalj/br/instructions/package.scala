/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Common instruction sequences.
 *
 * @author Michael Eichberg
 */
package object instructions {

    implicit final val TypeConversionInstructions: TypeConversionFactory[Array[Instruction]] =
        new TypeConversionFactory[Array[Instruction]] {

            final val NoConversion: Array[Instruction] = Array.empty

            final val IntToByte: Array[Instruction] = Array(I2B)
            final val IntToChar: Array[Instruction] = Array(I2C)
            final val IntToDouble: Array[Instruction] = Array(I2D)
            final val IntToFloat: Array[Instruction] = Array(I2F)
            final val IntToLong: Array[Instruction] = Array(I2L)
            final val IntToShort: Array[Instruction] = Array(I2S)

            final val Double2Byte: Array[Instruction] = Array(D2I, I2B)
            final val Double2Char: Array[Instruction] = Array(D2I, I2C)
            final val Double2Short: Array[Instruction] = Array(D2I, I2S)
            final val Double2Float: Array[Instruction] = Array(D2F)
            final val Double2Integer: Array[Instruction] = Array(D2I)
            final val Double2Long: Array[Instruction] = Array(D2L)

            final val Float2Byte: Array[Instruction] = Array(F2I, I2B)
            final val Float2Char: Array[Instruction] = Array(F2I, I2C)
            final val Float2Short: Array[Instruction] = Array(F2I, I2S)
            final val Float2Double: Array[Instruction] = Array(F2D)
            final val Float2Integer: Array[Instruction] = Array(F2I)
            final val Float2Long: Array[Instruction] = Array(F2L)

            final val Long2Byte: Array[Instruction] = Array(L2I, I2B)
            final val Long2Char: Array[Instruction] = Array(L2I, I2C)
            final val Long2Short: Array[Instruction] = Array(L2I, I2S)
            final val Long2Double: Array[Instruction] = Array(L2D)
            final val Long2Float: Array[Instruction] = Array(L2F)
            final val Long2Integer: Array[Instruction] = Array(L2I)

            final lazy val LangBooleanToPrimitiveBoolean: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Boolean,
                        "booleanValue",
                        MethodDescriptor.JustReturnsBoolean
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveBooleanToLangBoolean: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Boolean,
                        false,
                        "valueOf",
                        MethodDescriptor(BooleanType, ClassType.Boolean)
                    ),
                    null,
                    null
                )

            final lazy val LangLongToPrimitiveLong: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Long,
                        "longValue",
                        MethodDescriptor.JustReturnsLong
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveLongToLangLong: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Long,
                        false,
                        "valueOf",
                        MethodDescriptor(LongType, ClassType.Long)
                    ),
                    null,
                    null
                )

            final lazy val LangByteToPrimitiveByte: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Byte,
                        "byteValue",
                        MethodDescriptor.JustReturnsByte
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveByteToLangByte: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Byte,
                        false,
                        "valueOf",
                        MethodDescriptor(ByteType, ClassType.Byte)
                    ),
                    null,
                    null
                )

            final lazy val LangIntegerToPrimitiveInt: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Integer,
                        "intValue",
                        MethodDescriptor.JustReturnsInteger
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveIntToLangInteger: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Integer,
                        false,
                        "valueOf",
                        MethodDescriptor(IntegerType, ClassType.Integer)
                    ),
                    null,
                    null
                )

            final lazy val LangShortToPrimitiveShort: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Short,
                        "shortValue",
                        MethodDescriptor.JustReturnsShort
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveShortToLangShort: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Short,
                        false,
                        "valueOf",
                        MethodDescriptor(ShortType, ClassType.Short)
                    ),
                    null,
                    null
                )

            final lazy val LangFloatToPrimitiveFloat: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Float,
                        "floatValue",
                        MethodDescriptor.JustReturnsFloat
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveFloatToLangFloat: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Float,
                        false,
                        "valueOf",
                        MethodDescriptor(FloatType, ClassType.Float)
                    ),
                    null,
                    null
                )

            final lazy val LangCharacterToPrimitiveChar: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Character,
                        "charValue",
                        MethodDescriptor.JustReturnsChar
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveCharToLangCharacter: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Character,
                        false,
                        "valueOf",
                        MethodDescriptor(CharType, ClassType.Character)
                    ),
                    null,
                    null
                )

            final lazy val LangDoubleToPrimitiveDouble: Array[Instruction] =
                Array(
                    INVOKEVIRTUAL(
                        ClassType.Double,
                        "doubleValue",
                        MethodDescriptor.JustReturnsDouble
                    ),
                    null,
                    null
                )

            final lazy val PrimitiveDoubleToLangDouble: Array[Instruction] =
                Array(
                    INVOKESTATIC(
                        ClassType.Double,
                        false,
                        "valueOf",
                        MethodDescriptor(DoubleType, ClassType.Double)
                    ),
                    null,
                    null
                )

            private[this] lazy val unboxInstructions: Array[Array[Instruction]] = {
                val a = new Array[Array[Instruction]](ClassType.Double.id + 1)
                a(ClassType.Boolean.id) = LangBooleanToPrimitiveBoolean
                a(ClassType.Byte.id) = LangByteToPrimitiveByte
                a(ClassType.Character.id) = LangCharacterToPrimitiveChar
                a(ClassType.Short.id) = LangShortToPrimitiveShort
                a(ClassType.Integer.id) = LangIntegerToPrimitiveInt
                a(ClassType.Long.id) = LangLongToPrimitiveLong
                a(ClassType.Float.id) = LangFloatToPrimitiveFloat
                a(ClassType.Double.id) = LangDoubleToPrimitiveDouble
                a
            }

            def unboxValue(wrapperType: Type): Array[Instruction] = {
                val wid = wrapperType.id
                assert(wid >= ClassType.Boolean.id && wid <= ClassType.Double.id)

                unboxInstructions(wid)
            }
        }
}
