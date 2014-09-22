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

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

import scala.math.Ordered
import scala.collection.SortedSet

import org.opalj.collection.UID
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.CHECKCAST
import org.opalj.br.instructions.D2F
import org.opalj.br.instructions.D2I
import org.opalj.br.instructions.D2L
import org.opalj.br.instructions.F2D
import org.opalj.br.instructions.F2I
import org.opalj.br.instructions.F2L
import org.opalj.br.instructions.I2B
import org.opalj.br.instructions.I2C
import org.opalj.br.instructions.I2D
import org.opalj.br.instructions.I2F
import org.opalj.br.instructions.I2L
import org.opalj.br.instructions.I2S
import org.opalj.br.instructions.L2D
import org.opalj.br.instructions.L2F
import org.opalj.br.instructions.L2I

/**
 * Represents a JVM type.
 *
 * Programmatically, we distinguish three major kinds of types:
 *  - base types/primitive types
 *  - reference types
 *  - the type void.
 *
 * ==General Information==
 * ''From the JVM specification''
 *
 * There are three kinds of reference types: class types, array types, and interface
 * types. Their values are references to dynamically created class instances, arrays,
 * or class instances or arrays that implement interfaces, respectively.
 *
 * A reference value may also be the special null reference, a reference to no object,
 * which will be denoted here by null. The null reference initially has no runtime type,
 * but may be cast to any type. The default value of a reference type is null.
 * The Java virtual machine specification does not mandate a concrete value encoding null.
 *
 * ==Comparing Types/Performance==
 * Given that the comparison of types is a standard operation in static analysis that
 * is usually done over and over again great care was taken to enable an efficient
 * comparison of types. It is - '''without exception''' - always possible to compare
 * types using reference equality (i.e., the `eq`/`ne` operators). For each type there
 * will always be at most one object that represents that specific type.
 *
 * Additionally, a stable order is defined between types that is based on a type's
 * kind and the unique id of the types in case of reference types.
 * The order is:
 * ''void type &lt; primitive types &lt; array types &lt; class/interface types''
 *
 * @author Michael Eichberg
 */
sealed abstract class Type extends UID with Ordered[Type] {

    /**
     * Returns `true` if this type can be used by fields. Returns `true` unless
     * this type represents `void`.
     */
    def isFieldType: Boolean = false

    /**
     * Returns `true` if this type represents `void`; `false` otherwise.
     */
    def isVoidType: Boolean = false

    /**
     * Returns `true` if this type is a base type (also called primitive type).
     */
    def isBaseType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `byte`.
     */
    def isByteType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `char` (Range: [0..65535].
     */
    def isCharType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `short`.
     */
    def isShortType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `int`.
     */
    def isIntegerType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `long`.
     */
    def isLongType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `float`.
     */
    def isFloatType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `double`.
     */
    def isDoubleType: Boolean = false

    /**
     * Returns `true` if this type is the primitive type `boolean`.
     */
    def isBooleanType: Boolean = false

    /**
     * Returns `true` if this type is a reference type; that is, an array type or an
     * object type (class/interface type).
     *
     * @note
     * In general, we can distinguish the following three categories of types:
     *  - base types,
     *  - reference types,
     *  - the type void.
     */
    def isReferenceType: Boolean = false
    def isArrayType: Boolean = false
    def isObjectType: Boolean = false

    /**
     * The computational type of values of this type.
     */
    @throws[UnsupportedOperationException](
        "if this type has no associated computational type(i.e., if this type represents void)"
    )
    def computationalType: ComputationalType

    @throws[ClassCastException]("if this type is not a reference type")
    def asReferenceType: ReferenceType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to a ReferenceType")

    @throws[ClassCastException]("if this type is not an array type")
    def asArrayType: ArrayType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to an ArrayType")

    @throws[ClassCastException]("if this type is not an object type")
    def asObjectType: ObjectType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to an ObjectType")

    @throws[ClassCastException]("if this type is not a base type")
    def asBaseType: BaseType =
        throw new ClassCastException(
            "a "+this.getClass().getSimpleName()+" cannot be cast to a BaseType")

    @throws[ClassCastException]("if this type is not a field type")
    def asFieldType: FieldType =
        throw new ClassCastException(
            "a "+this.getClass().getSimpleName()+" cannot be cast to a FieldType")

    /**
     * A String representation of this type as it would be used in Java source code.
     */
    def toJava: String

    /**
     * Returns the binary name of this type as used by the Java runtime. Basically
     * returns the same name as produced by `Class.getName`.
     */
    @throws[UnsupportedOperationException](
        "if this type has not binary name(i.e., if this type represents void)"
    )
    def toBinaryJavaName: String

    /**
     * Returns the Java class object representing this type.
     *
     * '''This is generally only useful in very special cases and – to be meaningful at
     * all – it is necessary that the class path used for running the static analysis also
     * contains the classes that are analyzed. This is (often) only the case for the
     * JDK. '''
     *
     * However, one example where this is useful is the creation of a real object of
     * a specific type and to use that object when a method is called on that object.
     * This avoids the reimplementation of the respective logic as part of the analysis.
     * For example, if you want to get the `String` that is created by a specific
     * `StringBuffer` it is possible to implement the API of StringBuffer as part of
     * your analysis or (probably more efficient) to just create an instance of a
     * `StringBuffer` object and to redirect every call to the real object. In this case
     * only some general logic is required to redirect calls and to convert the values
     * between the representation used by the analysis and the representation required
     * by the called method.
     */
    def toJavaClass: java.lang.Class[_]

    /**
     * The unique id of this type. Types are associated with globally unique ids to
     * make it easy to define a global order.
     */
    def id: Int

    /**
     * Compares this type with the given type.
     *
     * Comparison of types is implemented by comparing the associated ids. I.e.,
     * the result of the comparison of two types is '''not stable''' across multiple
     * runs of OPAL.
     */
    override def compare(that: Type): Int = {
        if (this eq that)
            0
        else if (this.id < that.id)
            -1
        else
            1
    }

    override def <(other: Type) = this.id < other.id
    override def >(other: Type) = this.id > other.id
    override def >=(other: Type) = this.id >= other.id
    override def <=(other: Type) = this.id <= other.id

}

object ReturnType {

    def apply(rt: String): Type = if (rt.charAt(0) == 'V') VoidType else FieldType(rt)

}

/**
 * Represents the Java type/keyword `void`.
 *
 * @author Michael Eichberg
 */
sealed abstract class VoidType private () extends Type with ReturnTypeSignature {

    final val id = Int.MinValue

    final override def isVoidType = true

    final override def computationalType: ComputationalType =
        throw new UnsupportedOperationException("void does not have a computational type")

    final override def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJava: String = "void"

    override def toBinaryJavaName: String =
        throw new UnsupportedOperationException("void does not have a binary name")

    override def toJavaClass: java.lang.Class[_] = java.lang.Void.TYPE

    override def toString() = "VoidType"

}
case object VoidType extends VoidType

/**
 * Supertype of all types except [[VoidType]].
 *
 * @author Michael Eichberg
 */
sealed abstract class FieldType extends Type {

    final override def isFieldType = true

    final override def asFieldType: this.type = this

    /**
     * Returns the sequence of instructions that adapts values of `this` type to values
     * of the target type.
     *
     * This method supports the following kind of adaptations:
     *  - boxing
     *  - unboxing
     */
    @throws[IllegalArgumentException]("if a(n) (un)boxing to the targetType is not possible")
    def adapt(targetType: Type): Array[Instruction]
}
/**
 * Factory to parse field type (descriptors) to get field type objects.
 */
object FieldType {

    def apply(ft: String): FieldType = {
        (ft.charAt(0): @scala.annotation.switch) match {
            case 'B' ⇒ ByteType
            case 'C' ⇒ CharType
            case 'D' ⇒ DoubleType
            case 'F' ⇒ FloatType
            case 'I' ⇒ IntegerType
            case 'J' ⇒ LongType
            case 'S' ⇒ ShortType
            case 'Z' ⇒ BooleanType
            case 'L' ⇒ ObjectType(ft.substring(1, ft.length - 1))
            case '[' ⇒ ArrayType(FieldType(ft.substring(1)))
            case _   ⇒ throw new IllegalArgumentException(ft+" is not a valid field type descriptor")
        }
    }
}

sealed abstract class ReferenceType extends FieldType {

    final override def isReferenceType = true

    final override def asReferenceType: ReferenceType = this

    final override def computationalType = ComputationalTypeReference

    /**
     * Each reference type is associated with a unique id. Object types get ids &gt;= 0
     * and array types get ids &lt; 0.
     */
    def id: Int

}
/**
 * Factory to create instances of `ReferenceType`.
 */
object ReferenceType {

    /**
     * Creates a representation of the described [[ReferenceType]].
     *
     * @param rt A string as passed to `java.lang.Class.forName(...)`.
     */
    def apply(rt: String): ReferenceType = {
        if (rt.charAt(0) == '[')
            ArrayType(FieldType(rt.substring(1)))
        else
            ObjectType(rt)
    }
}

sealed abstract class BaseType extends FieldType with TypeSignature {

    final override def isBaseType = true

    final override def asBaseType: this.type = this

    /**
     * The atype value of the base type. The atype value uniquely identifies a base
     * type and is used primarily by the [instruction.NEWARRAY] instruction.
     */
    def atype: Int

    val WrapperType: ObjectType

    def boxValue: Array[Instruction]

    final override def adapt(targetType: Type): Array[Instruction] =
        if ((targetType eq `WrapperType`) || (targetType eq ObjectType.Object))
            boxValue
        else
            throw new IllegalArgumentException(
                s"adaptation of ${this.toJava} to ${targetType} is not supported"
            )
}

/**
 * Common constants related to base types (aka. primitive types).
 */
object BaseType {

    implicit val BaseTypeOrdering =
        new Ordering[BaseType] {
            def compare(a: BaseType, b: BaseType): Int = a.compare(b)
        }

    /**
     * The set of [BaseType]s sorted by the type's id.
     */
    final val baseTypes: SortedSet[BaseType] =
        SortedSet[BaseType](
            BooleanType,
            ByteType, CharType, ShortType, IntegerType, // <= "IntLike" values
            LongType,
            FloatType,
            DoubleType)
}

sealed abstract class NumericType protected () extends BaseType {

    /**
     * Returns the instruction sequence that can convert a value of the current
     * type to `targetType`.
     *
     * For primitive values the appropriate instructions that perform the necessary
     * widening/narrowing are returned. If this type
     * is a primitive type and the target type is a wrapper type, then the object of
     * the corresponding wrapper type is created and returned.
     *
     * @note The functionality implemented here, basically implements the logic
     *      for handling boxing and unboxing operations.
     *
     * @return This default implementation throws an `IllegalArgumentException`.
     */
    def convertTo(targetType: NumericType): Array[Instruction]

    /**
     * Determines if the range of values captured by `this` type is a '''strict'''
     * superset of the range of values captured by values of type `targetType`. Here,
     * strict superset means that – except of rounding issues – the value is conceptually
     * representable by `this` type. For example, a conversion from a `long` value to a
     * `double` value may loose some precision related to the least significant bits,
     * but the value is still representable.
     *
     * In general, the result of `isWiderThan` is comparable to the result of determing
     * if a conversion of a value of this type to the given type is an explicit/implicit
     * widening conversion.
     *
     * @example
     * {{{
     * assert(IntegerType.isWiderThan(IntegerType) == false)
     * assert(IntegerType.isWiderThan(LongType) == false)
     * assert(IntegerType.isWiderThan(ByteType) == true)
     * assert(LongType.isWiderThan(FloatType) == false)
     * assert(ByteType.isWiderThan(CharType) == false)
     * assert(LongType.isWiderThan(ShortType) == true)
     * }}}
     */
    def isWiderThan(targetType: NumericType): Boolean

}

object NumericType {

    final val IntToByte: Array[Instruction] = Array(I2B)
    final val IntToChar: Array[Instruction] = Array(I2C)
    final val IntToDouble: Array[Instruction] = Array(I2D)
    final val IntToFloat: Array[Instruction] = Array(I2F)
    final val IntToLong: Array[Instruction] = Array(I2L)
    final val IntToShort: Array[Instruction] = Array(I2S)

}

/**
 * An IntLikeType is every type (byte, char, short and int) that uses a primtive int
 * to store the current value and which has explicit support in the JVM.
 *
 * @note `Boolean` values are (at least conceptually) also stored in ints. However, the
 *      JVM has basically no explicit support for booleans (e.g., a conversion of an int
 *      value to a boolean is not directly supported).
 */
sealed abstract class IntLikeType protected () extends NumericType {

}

sealed abstract class ByteType private () extends IntLikeType {

    final val atype = 8

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Byte

    final override def isByteType = true

    final override def computationalType = ComputationalTypeInt

    def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "byte"

    override def toBinaryJavaName: String = "B"

    override def toJavaClass: java.lang.Class[_] = java.lang.Byte.TYPE

    override def toString() = "ByteType"

    override def isWiderThan(targetType: NumericType): Boolean = false

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id |
                ShortType.id |
                IntegerType.id ⇒ Array.empty
            case CharType.id   ⇒ NumericType.IntToChar
            case LongType.id   ⇒ NumericType.IntToLong
            case FloatType.id  ⇒ NumericType.IntToFloat
            case DoubleType.id ⇒ NumericType.IntToDouble
        }
    }

    override def boxValue: Array[Instruction] = ByteType.primitiveByteToLangByte

}
case object ByteType extends ByteType {

    final lazy val langByteToPrimitiveByte: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Byte,
                "byteValue",
                MethodDescriptor.JustReturnsByte),
            null,
            null)

    final lazy val primitiveByteToLangByte: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Byte,
                "valueOf",
                MethodDescriptor(ByteType, ObjectType.Byte)),
            null,
            null)
}

sealed abstract class CharType private () extends IntLikeType {

    final val atype = 5

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Character

    final override def isCharType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "char"

    override def toBinaryJavaName: String = "C"

    override def toJavaClass: java.lang.Class[_] = java.lang.Character.TYPE

    override def toString() = "CharType"

    override def isWiderThan(targetType: NumericType): Boolean = false

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id                  ⇒ NumericType.IntToByte
            case ShortType.id                 ⇒ NumericType.IntToShort
            case CharType.id | IntegerType.id ⇒ Array.empty
            case LongType.id                  ⇒ NumericType.IntToLong
            case FloatType.id                 ⇒ NumericType.IntToFloat
            case DoubleType.id                ⇒ NumericType.IntToDouble
        }
    }

    override def boxValue: Array[Instruction] = CharType.primitiveCharToLangCharacter

}
case object CharType extends CharType {

    final lazy val langCharacterToPrimitiveChar: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Character,
                "charValue",
                MethodDescriptor.JustReturnsChar),
            null,
            null)

    final lazy val primitiveCharToLangCharacter: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Character,
                "valueOf",
                MethodDescriptor(CharType, ObjectType.Character)),
            null,
            null)
}

sealed abstract class DoubleType private () extends NumericType {

    final override def isDoubleType = true

    final override def computationalType = ComputationalTypeDouble

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 7

    final val id = Int.MinValue + atype

    def toJava: String = "double"

    override def toBinaryJavaName: String = "D"

    final val WrapperType = ObjectType.Double

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Double.TYPE

    override def toString() = "DoubleType"

    override def isWiderThan(targetType: NumericType): Boolean = targetType ne this

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    ⇒ DoubleType.Double2Byte
            case CharType.id    ⇒ DoubleType.Double2Char
            case ShortType.id   ⇒ DoubleType.Double2Short
            case IntegerType.id ⇒ DoubleType.Double2Integer
            case LongType.id    ⇒ DoubleType.Double2Long
            case FloatType.id   ⇒ DoubleType.Double2Float
            case DoubleType.id  ⇒ Array.empty
        }
    }

    override def boxValue: Array[Instruction] = DoubleType.primitiveDoubleToLangDouble

}
case object DoubleType extends DoubleType {

    final val Double2Byte: Array[Instruction] = Array(D2I, I2B)
    final val Double2Char: Array[Instruction] = Array(D2I, I2C)
    final val Double2Short: Array[Instruction] = Array(D2I, I2S)
    final val Double2Float: Array[Instruction] = Array(D2F)
    final val Double2Integer: Array[Instruction] = Array(D2I)
    final val Double2Long: Array[Instruction] = Array(D2L)

    final lazy val langDoubleToPrimitiveDouble: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Double,
                "doubleValue",
                MethodDescriptor.JustReturnsDouble),
            null,
            null)

    final lazy val primitiveDoubleToLangDouble: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Double,
                "valueOf",
                MethodDescriptor(DoubleType, ObjectType.Double)),
            null,
            null)
}

sealed abstract class FloatType private () extends NumericType {

    final val atype = 6

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Float

    final override def isFloatType = true

    final override def computationalType = ComputationalTypeFloat

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "float"

    override def toBinaryJavaName: String = "F"

    override def toJavaClass: java.lang.Class[_] = java.lang.Float.TYPE

    override def toString() = "FloatType"

    override def isWiderThan(targetType: NumericType): Boolean =
        (targetType ne DoubleType) && (targetType ne this)

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    ⇒ FloatType.Float2Byte
            case CharType.id    ⇒ FloatType.Float2Char
            case ShortType.id   ⇒ FloatType.Float2Short
            case IntegerType.id ⇒ FloatType.Float2Integer
            case LongType.id    ⇒ FloatType.Float2Long
            case FloatType.id   ⇒ Array.empty
            case DoubleType.id  ⇒ FloatType.Float2Double
        }
    }

    override def boxValue: Array[Instruction] = FloatType.primitiveFloatToLangFloat

}
case object FloatType extends FloatType {

    final val Float2Byte: Array[Instruction] = Array(F2I, I2B)
    final val Float2Char: Array[Instruction] = Array(F2I, I2C)
    final val Float2Short: Array[Instruction] = Array(F2I, I2S)
    final val Float2Double: Array[Instruction] = Array(F2D)
    final val Float2Integer: Array[Instruction] = Array(F2I)
    final val Float2Long: Array[Instruction] = Array(F2L)

    final lazy val langFloatToPrimitiveFloat: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Float,
                "floatValue",
                MethodDescriptor.JustReturnsFloat),
            null,
            null)

    final lazy val primitiveFloatToLangFloat: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Float,
                "valueOf",
                MethodDescriptor(FloatType, ObjectType.Float)),
            null,
            null)
}

sealed abstract class ShortType private () extends IntLikeType {

    final val atype = 9

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Short

    final override def isShortType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "short"

    override def toBinaryJavaName: String = "S"

    override def toJavaClass: java.lang.Class[_] = java.lang.Short.TYPE

    override def toString() = "ShortType"

    override def isWiderThan(targetType: NumericType): Boolean = targetType eq ByteType

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    ⇒ NumericType.IntToByte
            case ShortType.id   ⇒ Array.empty
            case CharType.id    ⇒ NumericType.IntToChar
            case IntegerType.id ⇒ Array.empty
            case LongType.id    ⇒ NumericType.IntToLong
            case FloatType.id   ⇒ NumericType.IntToFloat
            case DoubleType.id  ⇒ NumericType.IntToDouble
        }
    }

    override def boxValue: Array[Instruction] = ShortType.primitiveShortToLangShort

}
case object ShortType extends ShortType {

    final lazy val langShortToPrimitiveShort: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Short,
                "shortValue",
                MethodDescriptor.JustReturnsShort),
            null,
            null)

    final lazy val primitiveShortToLangShort: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Short,
                "valueOf",
                MethodDescriptor(ShortType, ObjectType.Short)),
            null,
            null)
}

sealed abstract class IntegerType private () extends IntLikeType {

    final val atype = 10

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Integer

    final override def isIntegerType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "int"

    override def toBinaryJavaName: String = "I"

    override def toJavaClass: java.lang.Class[_] = java.lang.Integer.TYPE

    override def toString() = "IntegerType"

    override def isWiderThan(targetType: NumericType): Boolean =
        (targetType.id: @scala.annotation.switch) match {
            case ShortType.id | CharType.id | ByteType.id ⇒ true
            case _                                        ⇒ false
        }

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    ⇒ NumericType.IntToByte
            case ShortType.id   ⇒ NumericType.IntToShort
            case CharType.id    ⇒ NumericType.IntToChar
            case IntegerType.id ⇒ Array.empty
            case LongType.id    ⇒ NumericType.IntToLong
            case FloatType.id   ⇒ NumericType.IntToFloat
            case DoubleType.id  ⇒ NumericType.IntToDouble
        }
    }

    override def boxValue: Array[Instruction] = IntegerType.primitiveIntToLangInteger

}
case object IntegerType extends IntegerType {

    final lazy val langIntegerToPrimitiveInt: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Integer,
                "intValue",
                MethodDescriptor.JustReturnsInteger),
            null,
            null)

    final lazy val primitiveIntToLangInteger: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Integer,
                "valueOf",
                MethodDescriptor(IntegerType, ObjectType.Integer)),
            null,
            null)
}

sealed abstract class LongType private () extends NumericType {

    final val atype = 11

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Long

    final override def isLongType = true

    final override def computationalType = ComputationalTypeLong

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "long"

    override def toBinaryJavaName: String = "J"

    override def toJavaClass: java.lang.Class[_] = java.lang.Long.TYPE

    override def toString() = "LongType"

    override def isWiderThan(targetType: NumericType): Boolean =
        targetType.isInstanceOf[IntLikeType]

    override def convertTo(targetType: NumericType): Array[Instruction] = {
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    ⇒ LongType.Long2Byte
            case CharType.id    ⇒ LongType.Long2Char
            case ShortType.id   ⇒ LongType.Long2Short
            case IntegerType.id ⇒ LongType.Long2Integer
            case LongType.id    ⇒ Array.empty
            case FloatType.id   ⇒ LongType.Long2Float
            case DoubleType.id  ⇒ LongType.Long2Double
        }
    }

    override def boxValue: Array[Instruction] = LongType.primitiveLongToLangLong

}
case object LongType extends LongType {

    final val Long2Byte: Array[Instruction] = Array(L2I, I2B)
    final val Long2Char: Array[Instruction] = Array(L2I, I2C)
    final val Long2Short: Array[Instruction] = Array(L2I, I2S)
    final val Long2Double: Array[Instruction] = Array(L2D)
    final val Long2Float: Array[Instruction] = Array(L2F)
    final val Long2Integer: Array[Instruction] = Array(L2I)

    final lazy val langLongToPrimitiveLong: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Long,
                "longValue",
                MethodDescriptor.JustReturnsLong),
            null,
            null)

    final lazy val primitiveLongToLangLong: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Long,
                "valueOf",
                MethodDescriptor(LongType, ObjectType.Long)),
            null,
            null)
}

/**
 * The type of boolean values (true=1, false=0).
 *
 * Though the JVM internally uses an int value to store a boolean value the VM offers
 * no special further support for handling booleans. In particular the conversion of
 * some "byte|short|char|int" value to an int value is not directly supported.
 */
sealed abstract class BooleanType private () extends BaseType {

    final val atype = 4

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Boolean

    final override def isBooleanType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val toJava /*: String*/ = "boolean"

    override def toBinaryJavaName: String = "Z"

    override def toJavaClass: java.lang.Class[_] = java.lang.Boolean.TYPE

    override def toString() = "BooleanType"

    override def boxValue: Array[Instruction] = BooleanType.primitiveBooleanToLangBoolean

}
case object BooleanType extends BooleanType {

    final lazy val langBooleanToPrimitiveBoolean: Array[Instruction] =
        Array(
            INVOKEVIRTUAL(
                ObjectType.Boolean,
                "booleanValue",
                MethodDescriptor.JustReturnsBoolean),
            null,
            null)

    final lazy val primitiveBooleanToLangBoolean: Array[Instruction] =
        Array(
            INVOKESTATIC(
                ObjectType.Boolean,
                "valueOf",
                MethodDescriptor(BooleanType, ObjectType.Boolean)),
            null,
            null)
}

/**
 * Represents an `ObjectType`.
 *
 * @param id The unique id associated with this type.
 * @param fqn The fully qualified name of the class or interface in binary notation
 *      (e.g. "java/lang/Object").
 */
final class ObjectType private ( // DO NOT MAKE THIS A CASE CLASS!
    final val id: Int,
    final val fqn: String)
        extends ReferenceType {

    override def isObjectType: Boolean = true

    override def asObjectType: ObjectType = this

    @inline final def isPrimitiveTypeWrapper: Boolean = {
        val thisId = this.id
        thisId <= ObjectType.javaLangDoubleId && thisId >= ObjectType.javaLangBooleanId
    }

    final def isPrimitiveTypeWrapperOf(baseType: BaseType): Boolean =
        isPrimitiveTypeWrapper && (ObjectType.primitiveType(this).get eq baseType)

    def simpleName: String = ObjectType.simpleName(fqn)

    def packageName: String = ObjectType.packageName(fqn)

    override def toJava: String = fqn.replace('/', '.')

    override def toBinaryJavaName: String = "L"+toJava+";"

    override def toJavaClass: java.lang.Class[_] =
        classOf[Type].getClassLoader().loadClass(toJava)

    def unboxValue: Array[Instruction] = ObjectType.unboxValue(this)

    override def adapt(targetType: Type): Array[Instruction] =
        ObjectType.unboxValue(targetType)

    override def toString = "ObjectType("+fqn+")"

}
/**
 * Defines factory and extractor methods for `ObjectType`s.
 *
 * @author Michael Eichberg
 */
object ObjectType {

    import java.util.concurrent.atomic.AtomicInteger
    import java.util.concurrent.locks.ReentrantReadWriteLock
    import java.util.WeakHashMap
    import java.lang.ref.WeakReference

    private[this] val nextId = new AtomicInteger(0)
    private[this] val cacheRWLock = new ReentrantReadWriteLock();
    private[this] val cache = new WeakHashMap[String, WeakReference[ObjectType]]()

    @volatile private[this] var objectTypeCreationListener: ObjectType ⇒ Unit = null

    /**
     * Sets the listener and immediately calls it (multiple times) to inform the listener
     * about all known object types. It is guaranteed that the listener will not miss any
     * object type creation. However, invocation may occur concurrently.
     */
    def setObjectTypeCreationListener(f: ObjectType ⇒ Unit): Unit = {
        cacheRWLock.readLock().lock()
        try {
            objectTypeCreationListener = f
            val objectTypesIterator = cache.values().iterator()
            while (objectTypesIterator.hasNext) {
                val objectType = objectTypesIterator.next.get()
                if (objectType ne null) f(objectType)
            }
        } finally {
            cacheRWLock.readLock().unlock()
        }
    }

    /**
     * The number of different `ObjectType`s that were created.
     */
    def objectTypesCount = nextId.get

    /**
     * Factory method to create `ObjectType`s.
     *
     * @param fqn The fully qualified name of a class or interface type in
     *      binary notation.
     * @note `ObjectType` objects are cached internally to reduce the overall memory
     *      requirements and to ensure that only one instance of an `ObjectType` exists
     *      per fully qualified name. Hence, comparing `ObjectTypes` using reference
     *      comparison is explicitly supported.
     */
    def apply(fqn: String): ObjectType = {
        cacheRWLock.readLock().lock()
        try {
            val wrOT = cache.get(fqn)
            if (wrOT != null) {
                val OT = wrOT.get()
                if (OT != null)
                    return OT;
            }
        } finally {
            cacheRWLock.readLock().unlock()
        }

        cacheRWLock.writeLock().lock()
        try {
            // WE HAVE TO CHECK AGAIN
            val wrOT = cache.get(fqn)
            if (wrOT != null) {
                val OT = wrOT.get()
                if (OT != null)
                    return OT;
            }

            val newOT = new ObjectType(nextId.getAndIncrement(), fqn)
            val wrNewOT = new WeakReference(newOT)
            cache.put(fqn, wrNewOT)
            val currentObjectTypeCreationListener = objectTypeCreationListener
            if (currentObjectTypeCreationListener ne null)
                currentObjectTypeCreationListener(newOT)
            newOT
        } finally {
            cacheRWLock.writeLock().unlock()
        }
    }

    def unapply(ot: ObjectType): Option[String] = Some(ot.fqn)

    def simpleName(fqn: String): String = {
        val index = fqn.lastIndexOf('/')
        if (index > -1)
            fqn.substring(index + 1)
        else
            fqn
    }

    def packageName(fqn: String): String = {
        val index = fqn.lastIndexOf('/')
        if (index == -1)
            ""
        else
            fqn.substring(0, index)
    }

    final val Object = ObjectType("java/lang/Object")
    final val ObjectId = 0
    require(Object.id == ObjectId)

    final val Boolean = ObjectType("java/lang/Boolean")

    final val Byte = ObjectType("java/lang/Byte")
    final val Character = ObjectType("java/lang/Character")
    final val Short = ObjectType("java/lang/Short")
    final val Integer = ObjectType("java/lang/Integer")
    final val Long = ObjectType("java/lang/Long")
    final val Float = ObjectType("java/lang/Float")
    final val Double = ObjectType("java/lang/Double")
    require(Double.id - Boolean.id == 7)

    final val String = ObjectType("java/lang/String")
    final val StringId = 9
    require(String.id == StringId)

    final val Class = ObjectType("java/lang/Class")
    final val Throwable = ObjectType("java/lang/Throwable")
    final val Error = ObjectType("java/lang/Error")
    final val Exception = ObjectType("java/lang/Exception")
    final val RuntimeException = ObjectType("java/lang/RuntimeException")

    // Types related to the invokedynamic instruction
    final val MethodHandle = ObjectType("java/lang/invoke/MethodHandle")
    final val MethodHandles$Lookup = ObjectType("java/lang/invoke/MethodHandles$Lookup")
    final val MethodType = ObjectType("java/lang/invoke/MethodType")
    final val LambdaMetafactory = ObjectType("java/lang/invoke/LambdaMetafactory")
    final val CallSite = ObjectType("java/lang/invoke/CallSite")

    // Exceptions and errors that may be thrown by the JVM (i.e., instances of these 
    // exceptions may be created at runtime by the JVM)
    final val IndexOutOfBoundsException = ObjectType("java/lang/IndexOutOfBoundsException")
    final val ExceptionInInitializerError = ObjectType("java/lang/ExceptionInInitializerError")
    final val BootstrapMethodError = ObjectType("java/lang/BootstrapMethodError")
    final val OutOfMemoryError = ObjectType("java/lang/OutOfMemoryError")

    final val NullPointerException = ObjectType("java/lang/NullPointerException")
    final val ArrayIndexOutOfBoundsException = ObjectType("java/lang/ArrayIndexOutOfBoundsException")
    final val ArrayStoreException = ObjectType("java/lang/ArrayStoreException")
    final val NegativeArraySizeException = ObjectType("java/lang/NegativeArraySizeException")
    final val IllegalMonitorStateException = ObjectType("java/lang/IllegalMonitorStateException")
    final val ClassCastException = ObjectType("java/lang/ClassCastException")
    final val ArithmeticException = ObjectType("java/lang/ArithmeticException")
    final val ClassNotFoundException = ObjectType("java/lang/ClassNotFoundException")

    // the following types are relevant when checking the subtype relation between
    // two reference types where the subtype is an array type 
    final val Serializable = ObjectType("java/io/Serializable")
    final val Cloneable = ObjectType("java/lang/Cloneable")

    /**
     * Least upper type bound of Java arrays. That is, every Java array
     * is always `Serializable` and `Cloneable`.
     */
    final val SerializableAndCloneable: UIDSet[ObjectType] =
        UIDSet(ObjectType.Serializable, ObjectType.Cloneable)

    private final val javaLangBooleanId = Boolean.id
    private final val javaLangDoubleId = Double.id

    /**
     * Implicit mapping from a wrapper type to its primtive type.
     * @example
     * {{{
     * scala> import org.opalj.br._
     * scala> ObjectType.primitiveType(ObjectType.Integer.id)
     * res1: org.opalj.br.FieldType = IntegerType
     * }}}
     */
    private[this] lazy val primitiveType: Array[BaseType] = {
        val a = new Array[BaseType](Double.id + 1)
        a(Boolean.id) = BooleanType
        a(Byte.id) = ByteType
        a(Character.id) = CharType
        a(Short.id) = ShortType
        a(Integer.id) = IntegerType
        a(Long.id) = LongType
        a(Float.id) = FloatType
        a(Double.id) = DoubleType
        a
    }

    private[this] lazy val unboxInstructions: Array[Array[Instruction]] = {
        val a = new Array[Array[Instruction]](Double.id + 1)
        a(Boolean.id) = BooleanType.langBooleanToPrimitiveBoolean
        a(Byte.id) = ByteType.langByteToPrimitiveByte
        a(Character.id) = CharType.langCharacterToPrimitiveChar
        a(Short.id) = ShortType.langShortToPrimitiveShort
        a(Integer.id) = IntegerType.langIntegerToPrimitiveInt
        a(Long.id) = LongType.langLongToPrimitiveLong
        a(Float.id) = FloatType.langFloatToPrimitiveFloat
        a(Double.id) = DoubleType.langDoubleToPrimitiveDouble
        a
    }

    def unboxValue(wrapperType: Type): Array[Instruction] = {
        val wid = wrapperType.id
        if (wid >= Boolean.id && wid <= Double.id)
            unboxInstructions(wid)
        else
            throw new UnsupportedOperationException(
                s"unboxing ${wrapperType.toJava} values is not supported"
            )
    }

    /**
     * Given a wrapper type (e.g., `java.lang.Integer`) the underlying primitive type
     * is returned.
     *
     * @example
     * {{{
     * scala> import org.opalj.br._
     * scala> ObjectType.primitiveType(ObjectType.Integer)
     * res0: Option[org.opalj.br.BaseType] = Some(IntegerType)
     * }}}
     */
    def primitiveType(wrapperType: ObjectType): Option[BaseType] = {
        val wrapperId = wrapperType.id
        if (wrapperId < 0 || wrapperId > Double.id) {
            None
        } else {
            Some(primitiveType(wrapperId))
        }
    }

    def primitiveTypeWrapperMatcher[Args, T](
        booleanMatch: (Args) ⇒ T,
        byteMatch: (Args) ⇒ T,
        charMatch: (Args) ⇒ T,
        shortMatch: (Args) ⇒ T,
        integerMatch: (Args) ⇒ T,
        longMatch: (Args) ⇒ T,
        floatMatch: (Args) ⇒ T,
        doubleMatch: (Args) ⇒ T,
        orElse: (Args) ⇒ T): (ObjectType, Args) ⇒ T = {
        val fs = new Array[(Args) ⇒ T](8)
        fs(0) = booleanMatch
        fs(1) = byteMatch
        fs(2) = charMatch
        fs(3) = shortMatch
        fs(4) = integerMatch
        fs(5) = longMatch
        fs(6) = floatMatch
        fs(7) = doubleMatch

        (objectType: ObjectType, args: Args) ⇒ {
            val oid = objectType.id
            if (oid > javaLangDoubleId || oid < javaLangBooleanId) {
                orElse(args)
            } else {
                val index = oid - javaLangBooleanId
                fs(index)(args)
            }
        }
    }

    @inline final def isPrimitiveTypeWrapper(objectType: ObjectType): Boolean = {
        val oid = objectType.id
        oid <= javaLangDoubleId && oid >= javaLangBooleanId
    }
}

/**
 * Represents an array type.
 *
 * ==Comparing `ArrayType`s==
 * To facilitate comparisons of (array) types, each array type is represented
 * at any given time, by exactly one instance of `ArrayType`.
 *
 * ==General Information==
 * ''From the JVM specification''
 *
 * An array type consists of a '''component type''' with a single dimension (whose length is
 * not given by the type). The component type of an array type may itself be an array
 * type. If, starting from any array type, one considers its component type, and then
 * (if that is also an array type) the component type of that type, and so on, eventually
 * one must reach a component type that is not an array type; this is called the '''element
 * type of the array type'''. The element type of an array type is necessarily either a
 * primitive type, or a class type, or an interface type.
 *
 * @author Michael Eichberg
 */
final class ArrayType private ( // DO NOT MAKE THIS A CASE CLASS!
    val id: Int,
    val componentType: FieldType)
        extends ReferenceType {

    final override def isArrayType = true

    final override def asArrayType = this

    /**
     * Returns this array type's element type.
     *
     */
    def elementType: FieldType =
        componentType match {
            case at: ArrayType ⇒ at.elementType
            case _             ⇒ componentType
        }

    /**
     * The number of dimensions of this array. E.g. "Object[]" has one dimension and
     * "Object[][]" has two dimensions.
     */
    def dimensions: Int =
        1 + (componentType match { case at: ArrayType ⇒ at.dimensions; case _ ⇒ 0 })

    override def toJava: String = componentType.toJava+"[]"

    override def toBinaryJavaName: String = "["+componentType.toBinaryJavaName

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Class.forName(toBinaryJavaName)

    override def adapt(targetType: Type): Array[Instruction] =
        throw new UnsupportedOperationException("adaptation of array values is not supported")

    // The default equals and hashCode methods are a perfect fit.

    override def toString = "ArrayType("+componentType.toString+")"
}

/**
 * Defines factory and extractor methods for `ArrayType`s.
 *
 * @author Michael Eichberg
 */
object ArrayType {

    import java.util.concurrent.atomic.AtomicInteger
    import java.util.WeakHashMap
    import java.lang.ref.WeakReference

    private[this] val cache = new WeakHashMap[FieldType, WeakReference[ArrayType]]()

    private[this] val nextId = new AtomicInteger(-1)

    /**
     * Factory method to create objects of type `ArrayType`.
     *
     * ==Note==
     * `ArrayType` objects are cached internally to reduce the overall memory requirements
     * and to facilitate reference based comparisons. I.e., to `ArrayType`s are equal
     * iff it is the same object.
     */
    def apply(
        componentType: FieldType): ArrayType = {
        cache.synchronized {
            val wrAT = cache.get(componentType)
            if (wrAT != null) {
                val AT = wrAT.get()
                if (AT != null)
                    return AT;
            }
            val newAT = new ArrayType(nextId.getAndDecrement(), componentType)
            val wrNewAT = new WeakReference(newAT)
            cache.put(componentType, wrNewAT)
            newAT
        }
    }

    /**
     * Factory method to create an Array of the given component type with the given
     * dimension.
     */
    @annotation.tailrec def apply(
        dimension: Int,
        componentType: FieldType): ArrayType = {
        val at = apply(componentType)
        if (dimension > 1)
            apply(dimension - 1, at)
        else
            at
    }

    def unapply(at: ArrayType): Option[FieldType] = Some(at.componentType)

    final val ArrayOfObjects = ArrayType(ObjectType.Object)
}

/**
 * Facilitates matching against an array's element type.
 *
 * @author Michael Eichberg
 */
object ArrayElementType {
    def unapply(at: ArrayType): Option[FieldType] = Some(at.elementType)
}

/**
 * Defines an extractor to match a type against any `ObjectType`
 * except `java.lang.Object`.
 *
 * @author Michael Eichberg
 */
object NotJavaLangObject {

    def unapply(objectType: ObjectType): Boolean = objectType ne ObjectType.Object
}

/**
 * Defines an extractor to match against any `Type` except `void`. Can be useful, e.g.,
 * when matching `MethodDescriptor`s to select all methods that return something.
 *
 * @author Michael Eichberg
 */
object NotVoid {

    def unapply(someType: Type): Boolean = someType ne VoidType

}

