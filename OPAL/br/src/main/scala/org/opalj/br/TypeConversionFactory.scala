/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A factory to create instruction sequences to convert one primitive type into another one.
 *
 * @author Michael Eichberg
 */
trait TypeConversionFactory[T] {

    def NoConversion: T

    def IntToByte: T
    def IntToChar: T
    def IntToDouble: T
    def IntToFloat: T
    def IntToLong: T
    def IntToShort: T

    def Double2Byte: T
    def Double2Char: T
    def Double2Short: T
    def Double2Float: T
    def Double2Integer: T
    def Double2Long: T

    def Float2Byte: T
    def Float2Char: T
    def Float2Short: T
    def Float2Double: T
    def Float2Integer: T
    def Float2Long: T

    def Long2Byte: T
    def Long2Char: T
    def Long2Short: T
    def Long2Double: T
    def Long2Float: T
    def Long2Integer: T

    def LangBooleanToPrimitiveBoolean: T
    def PrimitiveBooleanToLangBoolean: T

    def LangByteToPrimitiveByte: T
    def PrimitiveByteToLangByte: T

    def LangCharacterToPrimitiveChar: T
    def PrimitiveCharToLangCharacter: T

    def LangShortToPrimitiveShort: T
    def PrimitiveShortToLangShort: T

    def LangIntegerToPrimitiveInt: T
    def PrimitiveIntToLangInteger: T

    def LangLongToPrimitiveLong: T
    def PrimitiveLongToLangLong: T

    def LangFloatToPrimitiveFloat: T
    def PrimitiveFloatToLangFloat: T

    def LangDoubleToPrimitiveDouble: T
    def PrimitiveDoubleToLangDouble: T

    def unboxValue(wrapperType: Type): T
}
