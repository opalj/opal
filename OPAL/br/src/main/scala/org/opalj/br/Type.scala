/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.annotation.tailrec

import java.lang.ref.WeakReference
import java.util.Arrays as JArrays
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.SortedSet
import scala.math.Ordered

import org.opalj.collection.UIDValue
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet2

/**
 * Represents a JVM type.
 *
 * Programmatically, we distinguish three major kinds of types:
 *  - base types/primitive types,
 *  - reference types,
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
 * @author Andre Pacak
 */
sealed trait Type extends UIDValue with Ordered[Type] {

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
     * Returns `true` if this type is the primitive type `char` (Range: [0..65535]).
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
     * Returns `true` if this type is a reference type; that is, an array type or a
     * class type (class/interface).
     *
     * @note
     * In general, we can distinguish the following three categories of types:
     *  - base types,
     *  - reference types,
     *  - the type void.
     */
    def isReferenceType: Boolean = false
    def isArrayType: Boolean = false
    def isClassType: Boolean = false
    def isNumericType: Boolean = false
    def isIntLikeType: Boolean = false

    /**
     * The computational type of values of this type.
     */
    @throws[UnsupportedOperationException](
        "if this type has no associated computational type(i.e., if this type represents void)"
    )
    def computationalType: ComputationalType

    /**
     * The number of operand stack slots/registers required to store
     * a single value of this type. In case of `VoidType` `0` is returned.
     */
    def operandSize: Int

    @throws[ClassCastException]("if this type is not a reference type")
    def asReferenceType: ReferenceType = {
        throw new ClassCastException(this.toJava + " cannot be cast to a ReferenceType");
    }

    @throws[ClassCastException]("if this type is not an array type")
    def asArrayType: ArrayType = {
        throw new ClassCastException(this.toJava + " cannot be cast to an ArrayType");
    }

    @throws[ClassCastException]("if this type is not a class type")
    def asClassType: ClassType = {
        throw new ClassCastException(this.toJava + " cannot be cast to a ClassType");
    }

    @throws[ClassCastException]("if this type is not a base type")
    def asBaseType: BaseType = {
        throw new ClassCastException(getClass.getSimpleName + " cannot be cast to a BaseType");
    }

    @throws[ClassCastException]("if this type is not a field type")
    def asFieldType: FieldType = {
        throw new ClassCastException(getClass.getSimpleName + " cannot be cast to a FieldType");
    }

    @throws[ClassCastException]("if this is not a numeric type")
    def asNumericType: NumericType = {
        throw new ClassCastException(getClass.getSimpleName + " cannot be cast to a NumericType");
    }

    @throws[ClassCastException]("if this is not an int like type")
    def asIntLikeType: IntLikeType = {
        throw new ClassCastException(getClass.getSimpleName + " cannot be cast to an IntLikeType");
    }

    @throws[ClassCastException]("if this is not a boolean type")
    def asBooleanType: BooleanType = {
        throw new ClassCastException(getClass.getSimpleName + " cannot be cast to an IntLikeType");
    }

    /**
     * A String representation of this type as it would be used in Java source code.
     */
    def toJava: String

    /**
     * Returns the binary name of this type as used by the Java runtime.
     * Returns the same name as produced by `Class.getName`.
     */
    def toBinaryJavaName: String = toJava

    /**
     * Returns the representation of this type as used by the JVM in, for example,
     * method descriptors or signatures.
     */
    def toJVMTypeName: String

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
    def toJavaClass: java.lang.Class[?]

    /**
     * The unique id of this type. Types are associated with globally unique ids to
     * make it easy to define a global order. The id Int.MinValue is used for Void;
     * Int.MinValue + 1 is used for CTIntType.
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

    override def <(other:  Type): Boolean = this.id < other.id
    override def >(other:  Type): Boolean = this.id > other.id
    override def >=(other: Type): Boolean = this.id >= other.id
    override def <=(other: Type): Boolean = this.id <= other.id

}

object Type {

    def apply(clazz: Class[?]): Type = {
        if (clazz.isPrimitive) {
            clazz match {
                case java.lang.Boolean.TYPE   => BooleanType
                case java.lang.Byte.TYPE      => ByteType
                case java.lang.Character.TYPE => CharType
                case java.lang.Short.TYPE     => ShortType
                case java.lang.Integer.TYPE   => IntegerType
                case java.lang.Long.TYPE      => LongType
                case java.lang.Float.TYPE     => FloatType
                case java.lang.Double.TYPE    => DoubleType
                case java.lang.Void.TYPE      => VoidType
                case _                        =>
                    throw new UnknownError(s"unknown primitive type $clazz")
            }
        } else {
            ReferenceType(clazz.getName.replace('.', '/'))
        }
    }
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

    override final def toJVMSignature: String = toJVMTypeName

    final val id = Int.MinValue

    final def WrapperType: ClassType = ClassType.Void

    override final def isVoidType: Boolean = true

    override final def computationalType: ComputationalType =
        throw new UnsupportedOperationException("void does not have a computational type")

    override final def operandSize: Int = 0

    override final def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJava: String = "void"

    override def toJVMTypeName: String = "V"

    override def toJavaClass: java.lang.Class[?] = java.lang.Void.TYPE

    override def toString: String = "VoidType"

}
case object VoidType extends VoidType

/**
 * Supertype of all types except [[VoidType]].
 *
 * @author Michael Eichberg
 */
sealed trait FieldType extends Type {

    override final def isFieldType: Boolean = true

    override final def asFieldType: this.type = this

    /**
     * Returns the sequence of instructions that adapts values of `this` type to values
     * of the target type.
     *
     * This method supports the following kind of adaptations:
     *  - boxing
     *  - unboxing
     */
    @throws[IllegalArgumentException]("if a(n) (un)boxing to the targetType is not possible")
    def adapt[T](targetType: Type)(implicit typeConversionFactory: TypeConversionFactory[T]): T
}

/**
 * Factory to parse field type (descriptors) to get field type objects.
 */
object FieldType {

    @throws[IllegalArgumentException](
        "if the given string is not a valid field type descriptor"
    )
    def apply(ft: String): FieldType = {
        (ft.charAt(0): @scala.annotation.switch) match {
            case 'B' => ByteType
            case 'C' => CharType
            case 'D' => DoubleType
            case 'F' => FloatType
            case 'I' => IntegerType
            case 'J' => LongType
            case 'S' => ShortType
            case 'Z' => BooleanType
            case 'L' => ClassType(ft.substring(1, ft.length - 1))
            case '[' => ArrayType(FieldType(ft.substring(1)))
            case _   => throw new IllegalArgumentException(ft + " is not a valid field type descriptor")
        }
    }
}

sealed abstract class ReferenceType extends FieldType {

    override final def isReferenceType: Boolean = true

    override final def asReferenceType: ReferenceType = this

    override final def computationalType: ComputationalType = ComputationalTypeReference

    override final def operandSize: Int = 1

    /**
     * Returns the most precise class type that represents this reference type. In
     * case of an `ArrayType`, the `ClassType` of `java.lang.Object` is returned;
     * other the current `ClassType`.
     */
    def mostPreciseClassType: ClassType

    /**
     * Each reference type is associated with a unique id. Class types get ids &gt;= 0
     * and array types get ids &lt; 0.
     */
    def id: Int

}

/**
 * Factory to create instances of `ReferenceType`.
 */
object ReferenceType {

    /**
     * Enables the reverse lookup of a ReferenceType given a ReferenceType's id.
     */
    def lookup(id: Int): ReferenceType = {
        if (id >= 0) ClassType.lookup(id)
        else ArrayType.lookup(id)
    }

    /**
     * Flushes the global caches for all ReferenceType instances. This does not include predefined
     * types, which are kept in memory for performance reasons.
     */
    def flushTypeCache(): Unit = {
        ClassType.flushTypeCache()
        ArrayType.flushTypeCache()
    }

    /**
     * Creates a representation of the described [[ReferenceType]].
     *
     * @param   rt A string as passed to `java.lang.Class.forName(...)` but in binary notation.
     *          Examples:
     *          {{{
     *          "[B" // in case of an array of Booleans
     *          "java/lang/Object" // for the class type java.lang.Object
     *          "[Ljava/lang/Object;" // for the array of java.lang.Object
     *          }}}
     */
    @throws[IllegalArgumentException]("in case of an invalid reference type descriptor")
    def apply(rt: String): ReferenceType = {
        if (rt.charAt(0) == '[')
            ArrayType(FieldType(rt.substring(1)))
        else
            ClassType(rt)
    }
}

sealed trait BaseType extends FieldType with TypeSignature {

    type JType <: AnyVal

    override final def isBaseType: Boolean = true

    override final def asBaseType: this.type = this

    override final def toJVMSignature: String = toJVMTypeName

    /**
     * The atype value of the base type. The atype value uniquely identifies a base
     * type and is used primarily by the [instruction.NEWARRAY] instruction.
     */
    def atype: Int

    val WrapperType: ClassType

    def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T

    override final def adapt[T](
        targetType: Type
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        if ((targetType eq WrapperType) || (targetType eq ClassType.Object)) {
            boxValue
        } else {
            val message = s"adaptation of ${this.toJava} to $targetType is not supported"
            throw new IllegalArgumentException(message)
        }
    }

}

/**
 * Common constants related to base types (aka. primitive types).
 */
object BaseType {

    implicit val BaseTypeOrdering: Ordering[BaseType] = (a: BaseType, b: BaseType) => a.compare(b)

    /**
     * The set of [BaseType]s sorted by the type's id.
     */
    final val baseTypes: SortedSet[BaseType] =
        SortedSet[BaseType](
            BooleanType, //

            ByteType,
            CharType,
            ShortType,
            IntegerType, // <= "IntLike" values

            LongType,
            FloatType,
            DoubleType
        )
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
     */
    def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T

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

    override def isNumericType = true

    override def asNumericType: this.type = this

}

/** All values which are stored in a value with computational type integer. */
sealed trait CTIntType extends Type {

    override final def computationalType: ComputationalType = ComputationalTypeInt

    override final def operandSize: Int = 1

}

/**
 * In some cases we abstract over all computational type integer values.
 */
case object CTIntType extends CTIntType {
    final val id = Int.MinValue + 1
    override def toBinaryJavaName: String = throw new UnsupportedOperationException()
    def toJVMTypeName: String = throw new UnsupportedOperationException()
    def toJava: String = throw new UnsupportedOperationException()
    def toJavaClass: Class[?] = throw new UnsupportedOperationException()
}

/**
 * An IntLikeType is every type (byte, char, short and int) that uses a primitive int
 * to store the current value and which has explicit support in the JVM.
 *
 * @note `Boolean` values are (at least conceptually) also stored in ints. However, the
 *      JVM has basically no explicit support for booleans (e.g., a conversion of an int
 *      value to a boolean is not directly supported).
 */
sealed abstract class IntLikeType protected () extends NumericType with CTIntType {

    override def isIntLikeType: Boolean = true

    override def asIntLikeType: this.type = this

}

sealed abstract class ByteType private () extends IntLikeType {

    override final type JType = Byte

    final val atype = 8

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Byte

    override final def isByteType: Boolean = true

    def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "byte"

    override def toJVMTypeName: String = "B"

    override def toJavaClass: java.lang.Class[?] = java.lang.Byte.TYPE

    override def toString: String = "ByteType"

    override def isWiderThan(targetType: NumericType): Boolean = false

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id |
                ShortType.id |
                IntegerType.id => NoConversion
            case CharType.id   => IntToChar
            case LongType.id   => IntToLong
            case FloatType.id  => IntToFloat
            case DoubleType.id => IntToDouble
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveByteToLangByte
    }

}
case object ByteType extends ByteType

sealed abstract class CharType private () extends IntLikeType {

    override final type JType = Char

    final val atype = 5

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Character

    override final def isCharType: Boolean = true

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "char"

    override def toJVMTypeName: String = "C"

    override def toJavaClass: java.lang.Class[?] = java.lang.Character.TYPE

    override def toString: String = "CharType"

    override def isWiderThan(targetType: NumericType): Boolean = false

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id                  => IntToByte
            case ShortType.id                 => IntToShort
            case CharType.id | IntegerType.id => NoConversion
            case LongType.id                  => IntToLong
            case FloatType.id                 => IntToFloat
            case DoubleType.id                => IntToDouble
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveCharToLangCharacter
    }

}
case object CharType extends CharType

sealed abstract class DoubleType private () extends NumericType {

    override final type JType = Double

    override final def isDoubleType: Boolean = true

    override final def computationalType: ComputationalType = ComputationalTypeDouble

    override final def operandSize: Int = 2

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 7

    final val id = Int.MinValue + atype

    def toJava: String = "double"

    override def toJVMTypeName: String = "D"

    final val WrapperType = ClassType.Double

    override def toJavaClass: java.lang.Class[?] =
        java.lang.Double.TYPE

    override def toString: String = "DoubleType"

    override def isWiderThan(targetType: NumericType): Boolean = targetType ne this

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    => Double2Byte
            case CharType.id    => Double2Char
            case ShortType.id   => Double2Short
            case IntegerType.id => Double2Integer
            case LongType.id    => Double2Long
            case FloatType.id   => Double2Float
            case DoubleType.id  => NoConversion
        }
    }

    override def boxValue[T](
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = { typeConversionFactory.PrimitiveDoubleToLangDouble }

}
case object DoubleType extends DoubleType

sealed abstract class FloatType private () extends NumericType {

    override final type JType = Float

    final val atype = 6

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Float

    override final def isFloatType: Boolean = true

    override final def computationalType: ComputationalType = ComputationalTypeFloat

    override final def operandSize: Int = 1

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "float"

    override def toJVMTypeName: String = "F"

    override def toJavaClass: java.lang.Class[?] = java.lang.Float.TYPE

    override def toString: String = "FloatType"

    override def isWiderThan(targetType: NumericType): Boolean =
        (targetType ne DoubleType) && (targetType ne this)

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    => Float2Byte
            case CharType.id    => Float2Char
            case ShortType.id   => Float2Short
            case IntegerType.id => Float2Integer
            case LongType.id    => Float2Long
            case FloatType.id   => NoConversion
            case DoubleType.id  => Float2Double
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveFloatToLangFloat
    }

}
case object FloatType extends FloatType

sealed abstract class ShortType private () extends IntLikeType {

    override final type JType = Short

    final val atype = 9

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Short

    override final def isShortType: Boolean = true

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "short"

    override def toJVMTypeName: String = "S"

    override def toJavaClass: java.lang.Class[?] = java.lang.Short.TYPE

    override def toString: String = "ShortType"

    override def isWiderThan(targetType: NumericType): Boolean = targetType eq ByteType

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    => IntToByte
            case ShortType.id   => NoConversion
            case CharType.id    => IntToChar
            case IntegerType.id => NoConversion
            case LongType.id    => IntToLong
            case FloatType.id   => IntToFloat
            case DoubleType.id  => IntToDouble
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveShortToLangShort
    }

}
case object ShortType extends ShortType

sealed abstract class IntegerType private () extends IntLikeType {

    override final type JType = Int

    final val atype = 10

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Integer

    override final def isIntegerType: Boolean = true

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "int"

    override def toJVMTypeName: String = "I"

    override def toJavaClass: java.lang.Class[?] = java.lang.Integer.TYPE

    override def toString: String = "IntegerType"

    override def isWiderThan(targetType: NumericType): Boolean =
        (targetType.id: @scala.annotation.switch) match {
            case ShortType.id | CharType.id | ByteType.id => true
            case _                                        => false
        }

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    => IntToByte
            case ShortType.id   => IntToShort
            case CharType.id    => IntToChar
            case IntegerType.id => NoConversion
            case LongType.id    => IntToLong
            case FloatType.id   => IntToFloat
            case DoubleType.id  => IntToDouble
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveIntToLangInteger
    }

}
case object IntegerType extends IntegerType

sealed abstract class LongType private () extends NumericType {

    override final type JType = Long

    final val atype = 11

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Long

    override final def isLongType: Boolean = true

    override final def computationalType: ComputationalType = ComputationalTypeLong

    override final def operandSize: Int = 2

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "long"

    override def toJVMTypeName: String = "J"

    override def toJavaClass: java.lang.Class[?] = java.lang.Long.TYPE

    override def toString: String = "LongType"

    override def isWiderThan(targetType: NumericType): Boolean =
        targetType.isInstanceOf[IntLikeType]

    override def convertTo[T](
        targetType: NumericType
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        import typeConversionFactory.*
        (targetType.id: @scala.annotation.switch) match {
            case ByteType.id    => Long2Byte
            case CharType.id    => Long2Char
            case ShortType.id   => Long2Short
            case IntegerType.id => Long2Integer
            case LongType.id    => NoConversion
            case FloatType.id   => Long2Float
            case DoubleType.id  => Long2Double
        }
    }

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveLongToLangLong
    }

}
case object LongType extends LongType

/**
 * The type of boolean values (true=1, false=0).
 *
 * Though the JVM internally uses an int value to store a boolean value the VM offers
 * no special further support for handling booleans. In particular the conversion of
 * some "byte|short|char|int" value to an int value is not directly supported.
 */
sealed abstract class BooleanType private () extends BaseType with CTIntType {

    override final type JType = Boolean

    final val atype = 4

    final val id = Int.MinValue + atype

    final val WrapperType = ClassType.Boolean

    override final def isBooleanType: Boolean = true

    override final def asBooleanType: BooleanType = this

    override final def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val toJava /*: String*/ = "boolean"

    override def toJVMTypeName: String = "Z"

    override def toJavaClass: java.lang.Class[?] = java.lang.Boolean.TYPE

    override def toString: String = "BooleanType"

    override def boxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        typeConversionFactory.PrimitiveBooleanToLangBoolean
    }

}
case object BooleanType extends BooleanType

/**
 * Represents an `ClassType`.
 *
 * @param id The unique id associated with this type.
 * @param fqn The fully qualified name of the class or interface in binary notation
 *      (e.g. "java/lang/Object").
 */
final class ClassType private ( // DO NOT MAKE THIS A CASE CLASS!
    final val id:  Int,
    final val fqn: String
) extends ReferenceType {

    assert(fqn.indexOf('.') == -1, s"invalid class type name: $fqn")

    final val packageName: String = ClassType.packageName(fqn).intern()

    override def isClassType: Boolean = true

    override def asClassType: ClassType = this

    override def mostPreciseClassType: ClassType = this

    @inline final def isPrimitiveTypeWrapper: Boolean = {
        val thisId = this.id
        thisId <= ClassType.javaLangDoubleId && thisId >= ClassType.javaLangBooleanId
    }

    final def isPrimitiveTypeWrapperOf(baseType: BaseType): Boolean =
        isPrimitiveTypeWrapper && (ClassType.primitiveType(this).get eq baseType)

    def simpleName: String = ClassType.simpleName(fqn)

    override def toJava: String = fqn.replace('/', '.')

    override def toJVMTypeName: String = s"L$fqn;"

    override def toJavaClass: java.lang.Class[?] = classOf[Type].getClassLoader.loadClass(toJava)

    def unboxValue[T](implicit typeConversionFactory: TypeConversionFactory[T]): T = {
        ClassType.unboxValue(this)
    }

    override def adapt[T](
        targetType: Type
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        ClassType.unboxValue(targetType)
    }

    def isASubtypeOf(that: ClassType)(implicit classHierarchy: ClassHierarchy): Answer = {
        classHierarchy.isASubtypeOf(this, that)
    }

    def isSubtypeOf(that: ClassType)(implicit classHierarchy: ClassHierarchy): Boolean = {
        classHierarchy.isSubtypeOf(this, that)
    }

    // The default equals and hashCode methods are a perfect fit.

    override def toString: String = "ClassType(" + fqn + ")"

}

/**
 * Defines factory and extractor methods for `ClassType`s.
 *
 * @author Michael Eichberg
 */
object ClassType {

    // IMPROVE Use a soft reference or something similar to avoid filling up the memory when we create multiple projects in a row!
    @volatile private var classTypes: Array[ClassType] = new Array[ClassType](0)

    private def updateClassTypes(): Unit = {
        if (nextId.get > classTypes.length) {
            val newClassTypes = JArrays.copyOf(this.classTypes, nextId.get)
            cacheRWLock.readLock().lock()
            try {
                cache.values.forEach { wct =>
                    val ct = wct.get
                    if (ct != null && ct.id < newClassTypes.length) {
                        newClassTypes(ct.id) = ct
                    }
                }
            } finally {
                cacheRWLock.readLock().unlock()
            }
            this.classTypes = newClassTypes
        }
    }

    /**
     * Enables the reverse lookup of a ClassType given a ClassType's id.
     */
    def lookup(id: Int): ClassType = {
        var classTypes = this.classTypes
        if (id < classTypes.length) {
            val ct = classTypes(id)
            if (ct == null) throw new IllegalArgumentException(s"$id is unknown")
            ct
        } else {
            // Let's check if the type was created in the meantime!
            updateClassTypes()
            classTypes = this.classTypes
            if (id < classTypes.length) {
                val ct = classTypes(id)
                if (ct == null) throw new IllegalArgumentException(s"$id is unknown")
                ct
            } else {
                throw new IllegalArgumentException(
                    s"$id belongs to ClassType created after the creation of the lookup map"
                )
            }
        }
    }

    /**
     *  Flushes the global cache for ClassType instances. This does not include the predefined types,
     *  which are kept in memory for performance reasons.
     */
    def flushTypeCache(): Unit = {

        val writeLock = cacheRWLock.writeLock()
        writeLock.lock()

        // First we need to write all cached new types to the actual array, otherwise
        // we might delete the predefined types
        updateClassTypes()

        try {
            // Clear the entire cache
            cache.clear()

            // Truncate the ClassType cache array to lose all not-predefined ClassTypes
            classTypes = JArrays.copyOf(classTypes, highestPredefinedTypeId + 1)

            // Refill the cache using the classTypes array
            classTypes.foreach { ct => cache.put(ct.fqn, new WeakReference[ClassType](ct)) }

            // Reset ID counter to highest id in the cache
            nextId.set(highestPredefinedTypeId + 1)
        } finally {
            writeLock.unlock()
        }

    }

    private val nextId = new AtomicInteger(0)
    private val cacheRWLock = new ReentrantReadWriteLock();
    private val cache = new WeakHashMap[String, WeakReference[ClassType]]()

    @volatile private var classTypeCreationListener: ClassType => Unit = _

    /**
     * Sets the listener and immediately calls it (multiple times) to inform the listener
     * about all known class types. It is guaranteed that the listener will not miss any
     * class type creation. However, invocation may occur concurrently.
     */
    def setClassTypeCreationListener(f: ClassType => Unit): Unit = {
        cacheRWLock.readLock().lock()
        try {
            classTypeCreationListener = f
            val classTypesIterator = cache.values().iterator()
            while (classTypesIterator.hasNext) {
                val classType = classTypesIterator.next.get()
                if (classType ne null) f(classType)
            }
        } finally {
            cacheRWLock.readLock().unlock()
        }
    }

    /**
     * The number of different `ClassType`s that were created.
     */
    def classTypesCount = nextId.get

    /**
     * Factory method to create `ClassType`s.
     *
     * @param  fqn The fully qualified name of a class or interface type in
     *         binary notation.
     * @note   `ClassType` objects are cached internally to reduce the overall memory
     *         requirements and to ensure that only one instance of an `ClassType` exists
     *         per fully qualified name. Hence, comparing `ClassTypes` using reference
     *         comparison is explicitly supported.
     */
    def apply(fqn: String): ClassType = {
        assert(!fqn.endsWith(";")) // Catch errors where we accidentally use a JVMTypeName instead

        val readLock = cacheRWLock.readLock()
        readLock.lock()
        try {
            val wrCT = cache.get(fqn)
            if (wrCT != null) {
                val CT = wrCT.get()
                if (CT != null)
                    return CT;
            }
        } finally {
            readLock.unlock()
        }

        // Remember: Lock upgrading is not possible
        val writeLock = cacheRWLock.writeLock()
        writeLock.lock()
        try {
            // WE HAVE TO CHECK AGAIN
            val wrCT = cache.get(fqn)
            if (wrCT != null) {
                val CT = wrCT.get()
                if (CT != null)
                    return CT;
            }

            val newCT = new ClassType(nextId.getAndIncrement(), fqn)
            val wrNewCT = new WeakReference(newCT)
            cache.put(fqn, wrNewCT)
            val currentClassTypeCreationListener = classTypeCreationListener
            if (currentClassTypeCreationListener ne null)
                currentClassTypeCreationListener(newCT)
            newCT
        } finally {
            writeLock.unlock()
        }
    }

    def unapply(ct: ClassType): Option[String] = Some(ct.fqn)

    def simpleName(fqn: String): String = {
        val index = fqn.lastIndexOf('/')
        if (index > -1)
            fqn.substring(index + 1)
        else
            fqn
    }

    /**
     * The package name of this type. The package name does not include
     * a final package separator char ("/").
     *
     * E.g.,
     * {{{
     * scala> val os = org.opalj.br.ClassType("java/lang/String")
     * os: org.opalj.br.ClassType = ClassType(java/lang/String)
     *
     * scala> os.packageName
     * res1: String = java/lang
     *
     * scala> os.simpleName
     * res2: String = String
     *
     * scala> os.toJava
     * res3: String = java.lang.String
     *
     * }}}
     */
    def packageName(fqn: String): String = {
        val index = fqn.lastIndexOf('/')
        if (index == -1)
            ""
        else
            fqn.substring(0, index)
    }

    // THE FOLLOWING CLASS TYPES ARE PREDEFINED BECAUSE OF
    // THEIR PERVASIVE USAGE AND THEIR EXPLICIT MENTIONING IN THE
    // THE JVM SPEC. OR THEIR IMPORTANCE FOR THE RUNTIME ENVIRONMENT
    final val Object = ClassType("java/lang/Object")
    final val ObjectId = 0
    require(Object.id == ObjectId)

    final val Boolean = ClassType("java/lang/Boolean")
    final val Byte = ClassType("java/lang/Byte")
    final val Character = ClassType("java/lang/Character")
    final val Short = ClassType("java/lang/Short")
    final val Integer = ClassType("java/lang/Integer")
    final val Long = ClassType("java/lang/Long")
    final val Float = ClassType("java/lang/Float")
    final val Double = ClassType("java/lang/Double")
    require(Double.id - Boolean.id == 7)

    final val Void = ClassType("java/lang/Void")

    final val String = ClassType("java/lang/String")
    final val StringId = 10
    require(String.id == StringId)

    final val Class = ClassType("java/lang/Class")
    final val ClassId = 11
    require(Class.id == ClassId)

    final val ClassLoader = ClassType("java/lang/ClassLoader")

    final val ModuleInfo = ClassType("module-info")
    require(ModuleInfo.id == 13)

    // the following types are relevant when checking the subtype relation between
    // two reference types where the subtype is an array type
    final val Serializable = ClassType("java/io/Serializable")
    final val SerializableId = 14
    require(Serializable.id == SerializableId)
    final val Cloneable = ClassType("java/lang/Cloneable")
    final val CloneableId = 15
    require(Cloneable.id == CloneableId)
    final val Comparable = ClassType("java/lang/Comparable")
    final val ComparableId = 16
    require(Comparable.id == ComparableId)
    final val StringBuilder = ClassType("java/lang/StringBuilder")
    final val StringBuilderId = 17
    require(StringBuilder.id == StringBuilderId)
    final val StringBuffer = ClassType("java/lang/StringBuffer")
    final val StringBufferId = 18
    require(StringBuffer.id == StringBufferId)

    final val System = ClassType("java/lang/System")

    final val Throwable = ClassType("java/lang/Throwable")
    final val Error = ClassType("java/lang/Error")
    final val Exception = ClassType("java/lang/Exception")
    final val RuntimeException = ClassType("java/lang/RuntimeException")

    final val Thread = ClassType("java/lang/Thread")
    final val ThreadGroup = ClassType("java/lang/ThreadGroup")
    final val Runnable = ClassType("java/lang/Runnable")

    final val Runtime = ClassType("java/lang/Runtime")
    final val Module = ClassType("java/lang/Module")

    // Types related to the invokedynamic instruction
    final val VarHandle = ClassType("java/lang/invoke/VarHandle")
    final val MethodHandle = ClassType("java/lang/invoke/MethodHandle")
    final val MethodHandles = ClassType("java/lang/invoke/MethodHandles")
    final val MethodHandles$Lookup = ClassType("java/lang/invoke/MethodHandles$Lookup")
    final val MethodType = ClassType("java/lang/invoke/MethodType")
    final val LambdaMetafactory = ClassType("java/lang/invoke/LambdaMetafactory")
    final val StringConcatFactory = ClassType("java/lang/invoke/StringConcatFactory")
    final val ObjectMethods = ClassType("java/lang/runtime/ObjectMethods")
    final val Objects = ClassType("java/util/Objects")
    final val CallSite = ClassType("java/lang/invoke/CallSite")
    final val ScalaLambdaDeserialize = ClassType("scala/runtime/LambdaDeserialize")
    final val SerializedLambda = ClassType("java/lang/invoke/SerializedLambda")
    final val ScalaSymbolLiteral = ClassType("scala/runtime/SymbolLiteral")
    final val ScalaSymbol = ClassType("scala/Symbol")
    final val ScalaStructuralCallSite = ClassType("scala/runtime/StructuralCallSite")
    final val ScalaRuntimeObject = ClassType("scala/runtime/ScalaRunTime$")
    final val Method = ClassType("java/lang/reflect/Method")
    final val Constructor = ClassType("java/lang/reflect/Constructor")
    final val Array = ClassType("java/lang/reflect/Array")
    final val Field = ClassType("java/lang/reflect/Field")
    final val AnnotatedElement = ClassType("java/lang/reflect/AnnotatedElement")
    final val GenericDeclaration = ClassType("java/lang/reflect/GenericDeclaration")
    final val Type = ClassType("java/lang/reflect/Type")

    // Types related to dynamic constants
    final val ConstantBootstraps = ClassType("java/lang/invoke/ConstantBootstraps")

    // Exceptions and errors that may be thrown by the JVM (i.e., instances of these
    // exceptions may be created at runtime by the JVM)
    final val IndexOutOfBoundsException = ClassType("java/lang/IndexOutOfBoundsException")
    final val ExceptionInInitializerError = ClassType("java/lang/ExceptionInInitializerError")
    final val BootstrapMethodError = ClassType("java/lang/BootstrapMethodError")
    final val OutOfMemoryError = ClassType("java/lang/OutOfMemoryError")

    final val NullPointerException = ClassType("java/lang/NullPointerException")
    final val ArrayIndexOutOfBoundsException = ClassType("java/lang/ArrayIndexOutOfBoundsException")
    final val ArrayStoreException = ClassType("java/lang/ArrayStoreException")
    final val NegativeArraySizeException = ClassType("java/lang/NegativeArraySizeException")
    final val IllegalMonitorStateException = ClassType("java/lang/IllegalMonitorStateException")
    final val ClassCastException = ClassType("java/lang/ClassCastException")
    final val ArithmeticException = ClassType("java/lang/ArithmeticException")
    final val ClassNotFoundException = ClassType("java/lang/ClassNotFoundException")
    final val IllegalArgumentException = ClassType("java/lang/IllegalArgumentException")
    final val IllegalStateException = ClassType("java/lang/IllegalStateException")

    final val AssertionError = ClassType("java/lang/AssertionError")
    /**
     * Least upper type bound of Java arrays. That is, every Java array
     * is always `Serializable` and `Cloneable`.
     */
    final val SerializableAndCloneable: UIDSet[ClassType] = {
        new UIDSet2(ClassType.Serializable, ClassType.Cloneable)
    }

    private final val javaLangBooleanId = Boolean.id
    private final val javaLangDoubleId = Double.id

    // Given the importance of "Object Serialization" we also predefine Externalizable
    final val Externalizable = ClassType("java/io/Externalizable")

    final val ObjectInputStream = ClassType("java/io/ObjectInputStream")
    final val ObjectOutputStream = ClassType("java/io/ObjectOutputStream")
    final val ObjectOutput = ClassType("java/io/ObjectOutput")
    final val ObjectInput = ClassType("java/io/ObjectInput")
    final val ObjectInputValidation = ClassType("java/io/ObjectInputValidation")

    // Java Security Types for doPrivileged analysis
    final val JavaSecurityAccessController = ClassType("java/security/AccessController")
    final val JavaSecurityPrivilegedAction = ClassType("java/security/PrivilegedAction")
    final val JavaSecurityPrivilegedExceptionAction = ClassType("java/security/PrivilegedExceptionAction")
    final val JavaSecurityAccessControlContext = ClassType("java/security/AccessControlContext")
    final val JavaSecurityPermission = ClassType("java/security/Permission")
    final val JavaSecurityCipher = ClassType("javax/crypto/Cipher")
    final val JavaSecurityKey = ClassType("java/security/Key")

    final val File = ClassType("java/io/File")
    final val PrintStream = ClassType("java/io/PrintStream")
    final val Collection = ClassType("java/util/Collection")
    final val Collections = ClassType("java/util/Collections")
    final val List = ClassType("java/util/List")
    final val ThreadUncaughtExceptionHandler = ClassType("java/lang/Thread$UncaughtExceptionHandler")
    final val Properties = ClassType("java/util/Properties")
    final val SunUnsafe = ClassType("sun/misc/Unsafe")
    final val JdkUnsafe = ClassType("jdk/internal/misc/Unsafe")

    private[br] final val highestPredefinedTypeId = nextId.get() - 1

    /**
     * Implicit mapping from a wrapper type to its primitive type.
     * @example
     * {{{
     * scala> import org.opalj.br.*
     * scala> ClassType.primitiveType(ClassType.Integer.id)
     * res1: org.opalj.br.FieldType = IntegerType
     * }}}
     */
    private lazy val primitiveType: Array[BaseType] = {
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

    def unboxValue[T](
        wrapperType: Type
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        typeConversionFactory.unboxValue(wrapperType)
    }

    /**
     * Given a wrapper type (e.g., `java.lang.Integer`) the underlying primitive type
     * is returned.
     *
     * @example
     * {{{
     * scala> import org.opalj.br.*
     * scala> ClassType.primitiveType(ClassType.Integer)
     * res0: Option[org.opalj.br.BaseType] = Some(IntegerType)
     * }}}
     */
    def primitiveType(wrapperType: ClassType): Option[BaseType] = {
        val wrapperId = wrapperType.id
        if (wrapperId < Boolean.id || wrapperId > Double.id) {
            None
        } else {
            Some(primitiveType(wrapperId))
        }
    }

    def primitiveTypeWrapperMatcher[Args, T](
        booleanMatch: Args => T,
        byteMatch:    Args => T,
        charMatch:    Args => T,
        shortMatch:   Args => T,
        integerMatch: Args => T,
        longMatch:    Args => T,
        floatMatch:   Args => T,
        doubleMatch:  Args => T,
        orElse:       Args => T
    ): (ClassType, Args) => T = {
        val fs = new Array[Args => T](8)
        fs(0) = booleanMatch
        fs(1) = byteMatch
        fs(2) = charMatch
        fs(3) = shortMatch
        fs(4) = integerMatch
        fs(5) = longMatch
        fs(6) = floatMatch
        fs(7) = doubleMatch

        (classType: ClassType, args: Args) => {
            val cid = classType.id
            if (cid > javaLangDoubleId || cid < javaLangBooleanId) {
                orElse(args)
            } else {
                val index = cid - javaLangBooleanId
                fs(index)(args)
            }
        }
    }

    @inline final def isPrimitiveTypeWrapper(classType: ClassType): Boolean = {
        val cid = classType.id
        cid <= javaLangDoubleId && cid >= javaLangBooleanId
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
    val id:            Int,
    val componentType: FieldType
) extends ReferenceType {

    override def isArrayType = true

    override def asArrayType = this

    override def mostPreciseClassType: ClassType = ClassType.Object

    /**
     * Returns this array type's element type. E.g., the element type of an
     * array of arrays of arrays of `int` is `int`.
     */
    def elementType: FieldType = {
        componentType match {
            case at: ArrayType => at.elementType
            case _             => componentType
        }
    }

    /**
     * The number of dimensions of this array. E.g. "Object[]" has one dimension and
     * "Object[][]" has two dimensions.
     */
    def dimensions: Int = {
        1 + (componentType match { case at: ArrayType => at.dimensions; case _ => 0 })
    }

    /**
     * Returns the component type of this array type after dropping the given number
     * of dimensions. E.g., if dimensions is `0`
     * `this` is returned; if it is `1`, then this array type's component type is returned.
     * If the value is larger than `1`, then the `componentType` has to be an array type
     * and `drop(dimensions-1)` will be called on that type.
     *
     * @param  dimensions The number of dimensions to drop. This values has to be equal or
     *         smaller than the number of dimensions of this array.
     */
    def drop(dimensions: Int): FieldType = {
        dimensions match {
            case 0 => this
            case 1 => this.componentType
            case _ => this.componentType.asArrayType.drop(dimensions - 1)
        }
    }

    override def toJava: String = componentType.toJava + "[]"

    override def toBinaryJavaName: String = "[" + componentType.toJVMTypeName.replace('/', '.')

    override def toJVMTypeName: String = "[" + componentType.toJVMTypeName

    override def toJavaClass: java.lang.Class[?] = java.lang.Class.forName(toBinaryJavaName)

    override def adapt[T](
        targetType: Type
    )(
        implicit typeConversionFactory: TypeConversionFactory[T]
    ): T = {
        throw new UnsupportedOperationException("adaptation of array values is not supported")
    }

    // The default equals and hashCode methods are a perfect fit.

    override def toString: String = "ArrayType(" + componentType.toString + ")"
}

/**
 * Defines factory and extractor methods for `ArrayType`s.
 *
 * @author Michael Eichberg
 */
object ArrayType {

    // IMPROVE Use a soft reference or something similar to avoid filling up the memory when we create multiple projects in a row!
    @volatile private var arrayTypes: Array[ArrayType] = new Array[ArrayType](0)

    private def updateArrayTypes(): Unit = {
        if (-nextId.get > arrayTypes.length) {
            val newArrayTypes = JArrays.copyOf(this.arrayTypes, -nextId.get)
            cache.synchronized {
                cache.values.forEach { wat =>
                    val at = wat.get
                    if (at != null && -at.id < newArrayTypes.length) {
                        newArrayTypes(-at.id) = at
                    }
                }
            }
            this.arrayTypes = newArrayTypes
        }
    }

    /**
     * Enables the reverse lookup of an ArrayType given an ArrayType's id.
     */
    def lookup(atId: Int): ArrayType = {
        var arrayTypes = this.arrayTypes
        val id = -atId
        if (id < arrayTypes.length) {
            val at = arrayTypes(id)
            if (at == null) throw new IllegalArgumentException(s"$atId is unknown")
            at
        } else {
            // Let's check if the type was created in the meantime!
            updateArrayTypes()
            arrayTypes = this.arrayTypes
            if (id < arrayTypes.length) {
                val at = arrayTypes(id)
                if (at == null) throw new IllegalArgumentException(s"$atId is unknown")
                at
            } else {
                throw new IllegalArgumentException(
                    s"$atId belongs to ArrayType created after the creation of the lookup map"
                )
            }
        }
    }

    /**
     *  Flushes the global cache for ArrayType instances. This does not include the predefined types,
     *  which are kept in memory for performance reasons.
     */
    def flushTypeCache(): Unit = {

        cache.synchronized {

            // First we need to write all cached new types to the actual array, otherwise
            // we might delete the predefined types
            updateArrayTypes()

            // Clear the entire cache
            cache.clear()

            // Reset array to only contain predefined ATs
            arrayTypes = JArrays.copyOf(arrayTypes, -lowestPredefinedTypeId + 1)

            // Refill the cache using the arrayTypes array
            arrayTypes.foreach { at =>
                // arrayTypes(0) is gonna be null, so we need this guard
                if (at != null) {
                    cache.put(at.componentType, new WeakReference[ArrayType](at))
                }
            }

            // Reset id counter
            nextId.set(lowestPredefinedTypeId - 1)

        }
    }

    private val cache = new WeakHashMap[FieldType, WeakReference[ArrayType]]()

    private val nextId = new AtomicInteger(-1)

    /**
     * Factory method to create objects of type `ArrayType`.
     *
     * ==Note==
     * `ArrayType` objects are cached internally to reduce the overall memory requirements
     * and to facilitate reference based comparisons. I.e., to `ArrayType`s are equal
     * iff it is the same object.
     */
    def apply(componentType: FieldType): ArrayType = {
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
    @tailrec def apply(dimension: Int, componentType: FieldType): ArrayType = {
        assert(dimension >= 1, s"dimension=$dimension, componentType=$componentType")

        val at = apply(componentType)
        if (dimension > 1)
            apply(dimension - 1, at)
        else
            at
    }

    def unapply(at: ArrayType): Option[FieldType] = Some(at.componentType)

    final val ArrayOfObject = ArrayType(ClassType.Object)
    final val ArrayOfMethodHandle = ArrayType(ClassType.MethodHandle)

    private[br] final val lowestPredefinedTypeId = nextId.get() + 1
}

/**
 * Facilitates matching against an array's element type.
 *
 * @author Michael Eichberg
 */
object ArrayElementType {

    def unapply(at: ArrayType): Option[FieldType] = Some(at.elementType)

}

object ElementReferenceType {

    def unapply(rt: ReferenceType): Option[ClassType] = {
        rt match {
            case ct: ClassType                   => Some(ct)
            case ArrayElementType(ct: ClassType) => Some(ct)
            case _                               => None
        }
    }

}

/**
 * Defines an extractor to match a type against any `ClassType` except `java.lang.Object`.
 *
 * @example
 * {{{
 * val t : Type = ...
 * t match {
 *  case ct @ NotJavaLangObject() => ct
 *  case _ =>
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
object NotJavaLangObject {

    def unapply(classType: ClassType): Boolean = classType ne ClassType.Object
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
