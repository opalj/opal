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

import org.opalj.collection.UID
import org.opalj.collection.immutable.UIDSet

/**
 * The computational type category of a value on the operand stack.
 *
 * (cf. JVM Spec. 2.11.1 Types and the Java Virtual Machine).
 *
 * @author Michael Eichberg
 */
sealed abstract class ComputationalTypeCategory(
        val operandSize: Byte) {
    /**
     * Identifies the computational type category.
     */
    val id: Byte
}
case object Category1ComputationalTypeCategory
        extends ComputationalTypeCategory(1) {
    final val id /*: Byte*/ = 1.toByte
}
case object Category2ComputationalTypeCategory
        extends ComputationalTypeCategory(2) {
    final val id /*: Byte*/ = 2.toByte
}

/**
 * The computational type of a value on the operand stack.
 *
 * (cf. JVM Spec. 2.11.1 Types and the Java Virtual Machine).
 */
sealed abstract class ComputationalType(
        computationTypeCategory: ComputationalTypeCategory) {

    def operandSize = computationTypeCategory.operandSize

    def isComputationalTypeReturnAddress: Boolean

    def category = computationTypeCategory.id

}
case object ComputationalTypeInt
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeFloat
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeReference
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeReturnAddress
        extends ComputationalType(Category1ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = true
}
case object ComputationalTypeLong
        extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}
case object ComputationalTypeDouble
        extends ComputationalType(Category2ComputationalTypeCategory) {
    def isComputationalTypeReturnAddress = false
}

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
sealed abstract class Type extends UID with scala.math.Ordered[Type] {

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
    def isByteType: Boolean = false
    def isCharType: Boolean = false
    def isShortType: Boolean = false
    def isIntegerType: Boolean = false
    def isLongType: Boolean = false
    def isFloatType: Boolean = false
    def isDoubleType: Boolean = false
    def isBooleanType: Boolean = false

    /**
     * Returns `true` if this type is a reference type; that is, an array type or an
     * object type (class/interface type).
     * Each type is either:
     *
     *  - a base type,
     *  - a reference type or
     *  - the type void.
     */
    def isReferenceType: Boolean = false
    def isArrayType: Boolean = false
    def isObjectType: Boolean = false

    def computationalType: ComputationalType

    def asReferenceType: ReferenceType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to a ReferenceType")

    def asArrayType: ArrayType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to an ArrayType")

    def asObjectType: ObjectType =
        throw new ClassCastException(
            "a "+this.getClass.getSimpleName+" cannot be cast to an ObjectType")

    /**
     * A String representation of this type as it would be used in Java source code.
     */
    def toJava: String

    /**
     * Returns the binary name of this type as used by the Java runtime. Basically
     * returns the same name as produced by `Class.getName`.
     */
    def toBinaryJavaName: String

    /**
     * Returns the Java class object representing this type.
     *
     * This is generally only useful in very special cases and – to be meaningful at all –
     * it is necessary that the class path used for running the static analysis also
     * contains the classes that are analyzed. This is (often) only the case for the JDK.
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

    override def compare(that: Type): Int = {
        if (this eq that)
            0
        else if (this.id < that.id)
            -1
        else
            1
    }

    /**
     * Compares this type with the other type by comparing their ids.
     */
    override def <(other: Type) = this.id < other.id
    override def >(other: Type) = this.id > other.id
    override def >=(other: Type) = this.id >= other.id
    override def <=(other: Type) = this.id <= other.id
}

object ReturnType {

    def apply(rt: String): Type = if (rt.charAt(0) == 'V') VoidType else FieldType(rt)

}

/**
 * Represents the Java keyword `void`.
 *
 * @author Michael Eichberg
 */
sealed abstract class VoidType private () extends Type with ReturnTypeSignature {

    final override def isVoidType = true

    final override def computationalType: ComputationalType =
        throw new UnsupportedOperationException("\"void\" does not have a computational type")

    final override def accept[T](sv: SignatureVisitor[T]): T = sv.visit(this)

    override def toJava: String = "void"

    override def toBinaryJavaName: String =
        throw new UnsupportedOperationException("the void type does not have a Java binary name")

    override def toJavaClass: java.lang.Class[_] = java.lang.Void.TYPE

    override def toString() = "VoidType"

    final val id = Int.MinValue
}
case object VoidType extends VoidType

/**
 * Supertype of all types except [[VoidType]].
 *
 * @author Michael Eichberg
 */
sealed abstract class FieldType extends Type {

    final override def isFieldType = true
}
/**
 * Factory object to parse field type (descriptors) to get field type objects.
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
 * Defines a factory method to create instances of `ReferenceType`
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

    def atype: Int

    val WrapperType: ObjectType

}

sealed abstract class ByteType private () extends BaseType {

    final override def isByteType = true

    final override def computationalType = ComputationalTypeInt

    final val atype = 8

    final val id = Int.MinValue + atype

    final val WrapperType = ObjectType.Byte

    def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    def toJava: String = "byte"

    override def toBinaryJavaName: String = "B"

    override def toJavaClass: java.lang.Class[_] = java.lang.Byte.TYPE

    override def toString() = "ByteType"
}
case object ByteType extends ByteType

sealed abstract class CharType private () extends BaseType {

    final override def isCharType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 5

    final val id = Int.MinValue + atype

    def toJava: String = "char"

    override def toBinaryJavaName: String = "C"

    final val WrapperType = ObjectType.Char

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Character.TYPE

    override def toString() = "CharType"

}
case object CharType extends CharType

sealed abstract class DoubleType private () extends BaseType {

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

}
case object DoubleType extends DoubleType

sealed abstract class FloatType private () extends BaseType {

    final override def isFloatType = true

    final override def computationalType = ComputationalTypeFloat

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 6

    final val id = Int.MinValue + atype

    def toJava: String = "float"

    override def toBinaryJavaName: String = "F"

    final val WrapperType = ObjectType.Float

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Float.TYPE

    override def toString() = "FloatType"

}
case object FloatType extends FloatType

sealed abstract class ShortType private () extends BaseType {

    final override def isShortType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 9

    final val id = Int.MinValue + atype

    def toJava: String = "short"

    override def toBinaryJavaName: String = "S"

    final val WrapperType = ObjectType.Short

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Short.TYPE

    override def toString() = "ShortType"

}
case object ShortType extends ShortType

sealed abstract class IntegerType private () extends BaseType {

    final override def isIntegerType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 10

    final val id = Int.MinValue + atype

    def toJava: String = "int"

    override def toBinaryJavaName: String = "I"

    final val WrapperType = ObjectType.Integer

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Integer.TYPE

    override def toString() = "IntegerType"

}
case object IntegerType extends IntegerType

sealed abstract class LongType private () extends BaseType {

    final override def isLongType = true

    final override def computationalType = ComputationalTypeLong

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 11

    final val id = Int.MinValue + atype

    def toJava: String = "long"

    override def toBinaryJavaName: String = "J"

    final val WrapperType = ObjectType.Long

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Long.TYPE

    override def toString() = "LongType"

}
case object LongType extends LongType

sealed abstract class BooleanType private () extends BaseType {

    final override def isBooleanType = true

    final override def computationalType = ComputationalTypeInt

    final override def accept[T](v: SignatureVisitor[T]): T = v.visit(this)

    final val atype = 4

    final val id = Int.MinValue + atype

    final val toJava /*: String*/ = "boolean"

    override def toBinaryJavaName: String = "Z"

    final val WrapperType = ObjectType.Boolean

    override def toJavaClass: java.lang.Class[_] =
        java.lang.Boolean.TYPE

    override def toString() = "BooleanType"

}
case object BooleanType extends BooleanType

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

    def simpleName: String = ObjectType.simpleName(fqn)

    def packageName: String = ObjectType.packageName(fqn)

    override def toJava: String = fqn.replace('/', '.')

    override def toBinaryJavaName: String = "L"+toJava+";"

    override def toJavaClass: java.lang.Class[_] =
        classOf[Type].getClassLoader().loadClass(toJava)

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
    final val Char = ObjectType("java/lang/Char")
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
        a(Char.id) = CharType
        a(Short.id) = ShortType
        a(Integer.id) = IntegerType
        a(Long.id) = LongType
        a(Float.id) = FloatType
        a(Double.id) = DoubleType
        a
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

    override def toJavaClass: java.lang.Class[_] = {
        java.lang.Class.forName(toBinaryJavaName)
    }

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

