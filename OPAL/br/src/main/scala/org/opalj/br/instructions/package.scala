/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Common instruction sequences.
 *
 * @author Michael Eichberg
 */
package object instructions {

    implicit final val TypeConversionInstructions = new TypeConversionFactory[Array[Instruction]] {

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
                    ObjectType.Boolean,
                    "booleanValue",
                    MethodDescriptor.JustReturnsBoolean
                ),
                null,
                null
            )

        final lazy val PrimitiveBooleanToLangBoolean: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Boolean,
                    false,
                    "valueOf",
                    MethodDescriptor(BooleanType, ObjectType.Boolean)
                ),
                null,
                null
            )

        final lazy val LangLongToPrimitiveLong: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Long,
                    "longValue",
                    MethodDescriptor.JustReturnsLong
                ),
                null,
                null
            )

        final lazy val PrimitiveLongToLangLong: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Long,
                    false,
                    "valueOf",
                    MethodDescriptor(LongType, ObjectType.Long)
                ),
                null,
                null
            )

        final lazy val LangByteToPrimitiveByte: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Byte,
                    "byteValue",
                    MethodDescriptor.JustReturnsByte
                ),
                null,
                null
            )

        final lazy val PrimitiveByteToLangByte: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Byte,
                    false,
                    "valueOf",
                    MethodDescriptor(ByteType, ObjectType.Byte)
                ),
                null,
                null
            )

        final lazy val LangIntegerToPrimitiveInt: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Integer,
                    "intValue",
                    MethodDescriptor.JustReturnsInteger
                ),
                null,
                null
            )

        final lazy val PrimitiveIntToLangInteger: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Integer,
                    false,
                    "valueOf",
                    MethodDescriptor(IntegerType, ObjectType.Integer)
                ),
                null,
                null
            )

        final lazy val LangShortToPrimitiveShort: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Short,
                    "shortValue",
                    MethodDescriptor.JustReturnsShort
                ),
                null,
                null
            )

        final lazy val PrimitiveShortToLangShort: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Short,
                    false,
                    "valueOf",
                    MethodDescriptor(ShortType, ObjectType.Short)
                ),
                null,
                null
            )

        final lazy val LangFloatToPrimitiveFloat: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Float,
                    "floatValue",
                    MethodDescriptor.JustReturnsFloat
                ),
                null,
                null
            )

        final lazy val PrimitiveFloatToLangFloat: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Float,
                    false,
                    "valueOf",
                    MethodDescriptor(FloatType, ObjectType.Float)
                ),
                null,
                null
            )

        final lazy val LangCharacterToPrimitiveChar: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Character,
                    "charValue",
                    MethodDescriptor.JustReturnsChar
                ),
                null,
                null
            )

        final lazy val PrimitiveCharToLangCharacter: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Character,
                    false,
                    "valueOf",
                    MethodDescriptor(CharType, ObjectType.Character)
                ),
                null,
                null
            )

        final lazy val LangDoubleToPrimitiveDouble: Array[Instruction] =
            Array(
                INVOKEVIRTUAL(
                    ObjectType.Double,
                    "doubleValue",
                    MethodDescriptor.JustReturnsDouble
                ),
                null,
                null
            )

        final lazy val PrimitiveDoubleToLangDouble: Array[Instruction] =
            Array(
                INVOKESTATIC(
                    ObjectType.Double,
                    false,
                    "valueOf",
                    MethodDescriptor(DoubleType, ObjectType.Double)
                ),
                null,
                null
            )

        private[this] lazy val unboxInstructions: Array[Array[Instruction]] = {
            val a = new Array[Array[Instruction]](ObjectType.Double.id + 1)
            a(ObjectType.Boolean.id) = LangBooleanToPrimitiveBoolean
            a(ObjectType.Byte.id) = LangByteToPrimitiveByte
            a(ObjectType.Character.id) = LangCharacterToPrimitiveChar
            a(ObjectType.Short.id) = LangShortToPrimitiveShort
            a(ObjectType.Integer.id) = LangIntegerToPrimitiveInt
            a(ObjectType.Long.id) = LangLongToPrimitiveLong
            a(ObjectType.Float.id) = LangFloatToPrimitiveFloat
            a(ObjectType.Double.id) = LangDoubleToPrimitiveDouble
            a
        }

        def unboxValue(wrapperType: Type): Array[Instruction] = {
            val wid = wrapperType.id
            assert(wid >= ObjectType.Boolean.id && wid <= ObjectType.Double.id)

            unboxInstructions(wid)
        }
    }
}
