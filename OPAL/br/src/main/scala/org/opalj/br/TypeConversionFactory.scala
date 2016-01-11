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
