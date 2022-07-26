/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.annotation.switch
import scala.collection.Seq
import scala.collection.immutable.ArraySeq
import scala.math.Ordered

/**
 * A method descriptor represents the parameters that the method takes and
 * the value that it returns.
 *
 * @note    The `equals(Any):Boolean` method takes the number of parameters and types
 *          into account. I.e., two method descriptor objects are equal if they have
 *          the same number of parameters and each parameter has the same [[Type]].
 *
 * @author Michael Eichberg
 */
sealed abstract class MethodDescriptor
    extends ConstantValue[MethodDescriptor]
    with (Int => FieldType)
    with Ordered[MethodDescriptor] {

    def parameterTypes: FieldTypes

    def apply(parameterIndex: Int): FieldType = {
        parameterTypes(parameterIndex)
    }

    def parameterType(index: Int): FieldType

    def parametersCount: Int

    final def copy(
        parameterTypes: FieldTypes = this.parameterTypes,
        returnType:     Type       = this.returnType
    ): MethodDescriptor = {
        MethodDescriptor(parameterTypes, returnType)
    }

    /**
     * The number of registers required to store the method parameters.
     *
     * @note An additional register may be required for storing the self
     *          reference `this`.
     */
    def requiredRegisters: Int = parameterTypes.foldLeft(0)(_ + _.operandSize)

    def returnType: Type

    def toJVMDescriptor: String = {
        parameterTypes.iterator
            .map[String](_.toJVMTypeName)
            .mkString("(", "", ")"+returnType.toJVMTypeName)
    }

    def value: this.type = this

    override def runtimeValueType: ObjectType = ObjectType.MethodType

    def valueToString: String = toUMLNotation

    /**
     * Returns a Java like view when a MethodDescriptor is used as a [[BootstrapArgument]].
     */
    def toJava: String = {
        val parameterTypes =
            this.parameterTypes.iterator.map[String](_.toJava).mkString("(", ",", ")")
        s"MethodDescriptor(${returnType.toJava},$parameterTypes)"
    }

    def equalParameters(other: MethodDescriptor): Boolean

    /**
     * Selects the indexes of the parameters that pass the filter function.
     *
     * @note This index is not necessarily identical to the value used to identify
     *      the origin of value (a parameter passed to a method).
     */
    def selectParameter(f: FieldType => Boolean): Seq[Int] = {
        var i = 0
        val max = parametersCount
        var indexes: Seq[Int] = Nil
        while (i < max) {
            if (f(parameterType(i))) {
                indexes = indexes :+ i
            }
            i += 1
        }
        indexes
    }

    /**
     * @return `true` iff a parameter – except of the last one – is a computational type category
     *        2 value; i.e., is a long or double value. If all values are category 1 values, then
     *        the parameters are store in the first n registers/local variables.
     *
     */
    def hasComputationalTypeCategory2ValueInInit: Boolean

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toJava(methodName: String): String = {
        parameterTypes.view.map(_.toJava).mkString(s"${returnType.toJava} $methodName(", ",", ")")
    }

    def toJava(declaringClassName: String, methodName: String): String = {
        s"$declaringClassName{ ${toJava(methodName)} }"
    }

    def toUMLNotation: String = {
        "("+{
            if (parameterTypes.isEmpty)
                ""
            else
                parameterTypes.tail.foldLeft(parameterTypes.head.toJava)(_+", "+_.toJava)
        }+"): "+returnType.toJava
    }

    override def <(other: MethodDescriptor): Boolean = {
        val thisParametersCount = this.parametersCount
        val otherParametersCount = other.parametersCount

        (thisParametersCount < otherParametersCount) || (
            thisParametersCount == otherParametersCount &&
            {
                var i = 0
                val iMax = this.parametersCount
                while (i < iMax) {
                    if (this.parameterTypes(i) < other.parameterTypes(i))
                        return true;
                    else if (other.parameterTypes(i) < this.parameterTypes(i))
                        return false;
                    else // the types are identical
                        i += 1
                }
                this.returnType < other.returnType
            }
        )
    }

    override def toString: String = "MethodDescriptor("+toUMLNotation+")"
}

//
// To optimize the overall memory consumption and to facilitate the storage of
// method descriptors in sets, we have specialized the MethodDescriptor.
// (Done after a study of the heap memory usage.)
//

private object NoArgumentAndNoReturnValueMethodDescriptor extends MethodDescriptor {

    override def returnType: VoidType = VoidType

    override def parameterTypes: FieldTypes = NoFieldTypes

    override def parameterType(index: Int): FieldType = throw new IndexOutOfBoundsException()

    override def parametersCount: Int = 0

    override def hasComputationalTypeCategory2ValueInInit: Boolean = false

    override def equalParameters(other: MethodDescriptor): Boolean = {
        other.parametersCount == 0
    }

    override def compare(other: MethodDescriptor): Int = {
        if (other == NoArgumentAndNoReturnValueMethodDescriptor)
            0
        else
            -1
    }

    // the default equals and hashCode implementations are a perfect fit
}

private final class NoArgumentMethodDescriptor(val returnType: Type) extends MethodDescriptor {

    override def parameterTypes: FieldTypes = NoFieldTypes

    override def parameterType(index: Int): FieldType = throw new IndexOutOfBoundsException()

    override def parametersCount: Int = 0

    override def hasComputationalTypeCategory2ValueInInit: Boolean = false

    override def equalParameters(other: MethodDescriptor): Boolean =
        other.parametersCount == 0

    override def hashCode: Int = returnType.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: NoArgumentMethodDescriptor => that.returnType eq this.returnType
            case _                                => false
        }
    }

    override def compare(other: MethodDescriptor): Int = {
        (other.parametersCount: @switch) match {
            case 0 => this.returnType.compare(other.returnType)
            case _ => -1
        }
    }

}

private final class SingleArgumentMethodDescriptor(
        val parameterType: FieldType,
        val returnType:    Type
) extends MethodDescriptor {

    override def parameterTypes: FieldTypes = ArraySeq(parameterType)

    override def parameterType(index: Int): FieldType = {
        if (index == 0)
            parameterType
        else
            throw new IndexOutOfBoundsException()
    }

    override def parametersCount: Int = 1

    override def hasComputationalTypeCategory2ValueInInit: Boolean = false

    override def equalParameters(other: MethodDescriptor): Boolean = {
        (other.parametersCount == 1) && (other.parameterType(0) == parameterType)
    }

    override lazy val hashCode: Int = (returnType.hashCode() * 61) + parameterType.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: SingleArgumentMethodDescriptor =>
                (that.parameterType eq this.parameterType) && (that.returnType eq this.returnType)
            case _ =>
                false
        }
    }

    override def compare(other: MethodDescriptor): Int = {
        (other.parametersCount: @switch) match {
            case 0 => 1
            case 1 =>
                val c = parameterType compare other.parameterType(0)
                if (c != 0)
                    c
                else
                    this.returnType.compare(other.returnType)
            case _ => -1
        }
    }

}

private final class TwoArgumentsMethodDescriptor(
        val firstParameterType:  FieldType,
        val secondParameterType: FieldType,
        val returnType:          Type
) extends MethodDescriptor {

    override def parameterTypes: FieldTypes = ArraySeq(firstParameterType, secondParameterType)

    override def parameterType(index: Int): FieldType = {
        (index: @switch) match {
            case 0 => firstParameterType
            case 1 => secondParameterType
            case _ => throw new IndexOutOfBoundsException()
        }
    }

    override def parametersCount: Int = 2

    override def hasComputationalTypeCategory2ValueInInit: Boolean = {
        firstParameterType.computationalType.categoryId == 2
    }

    override def compare(other: MethodDescriptor): Int = {
        (other.parametersCount: @switch) match {
            case 0 | 1 => 1
            case 2 =>
                var c = firstParameterType compare other.parameterType(0)
                if (c != 0)
                    c
                else {
                    c = secondParameterType compare other.parameterType(1)
                    if (c != 0) {
                        c
                    } else {
                        this.returnType.compare(other.returnType)
                    }
                }
            case _ => -1
        }
    }

    override def equalParameters(other: MethodDescriptor): Boolean = {
        (other.parametersCount == 2) &&
            (other.parameterType(0) == firstParameterType) &&
            (other.parameterType(1) == secondParameterType)
    }

    override lazy val hashCode: Int = {
        (firstParameterType.hashCode * 13 + secondParameterType.hashCode) * 61 +
            returnType.hashCode()
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: TwoArgumentsMethodDescriptor =>
                (that.firstParameterType eq this.firstParameterType) &&
                    (that.secondParameterType eq this.secondParameterType) &&
                    (that.returnType eq this.returnType)
            case _ =>
                false
        }
    }
}

private final class MultiArgumentsMethodDescriptor(
        val parameterTypes: FieldTypes,
        val returnType:     Type
) extends MethodDescriptor {

    override def parameterType(index: Int): FieldType = parameterTypes(index)

    override def parametersCount: Int = parameterTypes.size

    override def hasComputationalTypeCategory2ValueInInit: Boolean = {
        var i = 0
        val max = parameterTypes.size - 1
        while (i < max) {
            if (parameterTypes(i).computationalType.categoryId == 2)
                return true;
            i += 1
        }
        false

    }

    override def compare(other: MethodDescriptor): Int = {
        val thisParametersCount = this.parametersCount
        val otherParametersCount = other.parametersCount
        if (thisParametersCount < otherParametersCount)
            -1
        else if (thisParametersCount > otherParametersCount)
            1
        else {
            var i = 0
            while (i < thisParametersCount) {
                val comparisonResult = this.parameterTypes(i).compare(other.parameterTypes(i))
                if (comparisonResult != 0)
                    return comparisonResult;
                else // the types are identical
                    i += 1
            }
            this.returnType.compare(other.returnType)
        }
    }

    override def equalParameters(other: MethodDescriptor): Boolean = {
        other.parameterTypes == this.parameterTypes
    }

    override lazy val hashCode: Int = (returnType.hashCode * 13) + parameterTypes.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: MethodDescriptor =>
                (this.returnType eq that.returnType) &&
                    this.parametersCount == that.parametersCount &&
                    {
                        var i = parametersCount
                        while (i > 0) {
                            i = i - 1
                            if (this.parameterTypes(i) ne that.parameterTypes(i))
                                return false;
                        }
                        true
                    }
            case _ =>
                false
        }
    }
}

object HasNoArgsAndReturnsVoid {

    def unapply(md: MethodDescriptor): Boolean = {
        md match {
            case NoArgumentAndNoReturnValueMethodDescriptor => true
            case _                                          => false
        }
    }
}

object NoArgumentMethodDescriptor {

    def unapply(md: MethodDescriptor): Option[Type] = {
        md match {
            case md: NoArgumentMethodDescriptor => Some(md.returnType)
            case _                              => None
        }
    }
}

/**
 * Extractor for method descriptors defining a single parameter type and some return type.
 *
 * @author Michael Eichberg
 */
object SingleArgumentMethodDescriptor {

    def apply(parameterType: FieldType, returnType: Type = VoidType): MethodDescriptor = {
        new SingleArgumentMethodDescriptor(parameterType, returnType)
    }

    def unapply(md: MethodDescriptor): Option[(FieldType, Type)] = {
        md match {
            case md: SingleArgumentMethodDescriptor => Some((md.parameterType, md.returnType))
            case _                                  => None
        }
    }
}

/**
 * Extractor for method descriptors defining a single parameter type.
 *
 * @author Michael Eichberg
 */
object TheArgument {

    /**
     * Returns `Some(FieldType)` of the first paramter type if the given method
     * descriptor just defines a single paramter.
     *
     * @author Michael Eichberg
     */
    def unapply(md: MethodDescriptor): Option[FieldType] = {
        if (md.parametersCount == 1) {
            Some(md.parameterType(0))
        } else {
            None
        }
    }

}

object TwoArgumentsMethodDescriptor {

    def unapply(md: MethodDescriptor): Option[(FieldType, FieldType, Type)] = {
        md match {
            case md: TwoArgumentsMethodDescriptor =>
                Some((md.firstParameterType, md.secondParameterType, md.returnType))
            case _ =>
                None
        }
    }
}

/**
 * Defines extractor and factory methods for MethodDescriptors.
 *
 * @author Michael Eichberg
 */
object MethodDescriptor {

    def unapply(md: MethodDescriptor): Option[(FieldTypes, Type)] = {
        Some((md.parameterTypes, md.returnType))
    }

    final val NoArgsAndReturnVoid: MethodDescriptor = NoArgumentAndNoReturnValueMethodDescriptor

    final def DefaultConstructorDescriptor: MethodDescriptor = {
        NoArgumentAndNoReturnValueMethodDescriptor
    }

    /**
     * The signatures of a signature polymorphic method.
     * Basically, the signature is one of:
     * {{{
     *      (params: Object[]) : Object
     *      (params: Object[]) : void
     *      (params: Object[]) : boolean
     * }}}
     */
    final val SignaturePolymorphicMethodObject: MethodDescriptor = {
        new SingleArgumentMethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
    }

    final val SignaturePolymorphicMethodVoid: MethodDescriptor = {
        new SingleArgumentMethodDescriptor(ArrayType.ArrayOfObject, VoidType)
    }

    final val SignaturePolymorphicMethodBoolean: MethodDescriptor = {
        new SingleArgumentMethodDescriptor(ArrayType.ArrayOfObject, BooleanType)
    }

    final val JustReturnsBoolean: MethodDescriptor = new NoArgumentMethodDescriptor(BooleanType)

    final val JustReturnsByte: MethodDescriptor = new NoArgumentMethodDescriptor(ByteType)

    final val JustReturnsShort: MethodDescriptor = new NoArgumentMethodDescriptor(ShortType)

    final val JustReturnsChar: MethodDescriptor = new NoArgumentMethodDescriptor(CharType)

    final val JustReturnsInteger: MethodDescriptor = new NoArgumentMethodDescriptor(IntegerType)

    final val JustReturnsFloat: MethodDescriptor = new NoArgumentMethodDescriptor(FloatType)

    final val JustReturnsDouble: MethodDescriptor = new NoArgumentMethodDescriptor(DoubleType)

    final val JustReturnsLong: MethodDescriptor = new NoArgumentMethodDescriptor(LongType)

    final val JustReturnsObject: MethodDescriptor = {
        new NoArgumentMethodDescriptor(ObjectType.Object)
    }

    final val JustReturnsClass: MethodDescriptor = {
        new NoArgumentMethodDescriptor(ObjectType.Class)
    }

    final val JustReturnsString: MethodDescriptor = {
        new NoArgumentMethodDescriptor(ObjectType.String)
    }

    final val JustTakesObject: MethodDescriptor = apply(ObjectType.Object, VoidType)

    def JustTakes(parameterType: FieldType): MethodDescriptor = {
        new SingleArgumentMethodDescriptor(parameterType, VoidType)
    }

    final val ReadObjectDescriptor = {
        MethodDescriptor(ObjectType("java/io/ObjectInputStream"), VoidType)
    }

    final val WriteObjectDescriptor = {
        MethodDescriptor(ObjectType("java/io/ObjectOutputStream"), VoidType)
    }

    final val ReadObjectInputDescriptor = {
        MethodDescriptor(ObjectType("java/io/ObjectInput"), VoidType)
    }

    final val WriteObjectOutputDescriptor = {
        MethodDescriptor(ObjectType("java/io/ObjectOutput"), VoidType)
    }

    /**
     * Descriptor of the method `java.lang.invoke.LambdaMetafactory.metafactory`.
     */
    final val LambdaMetafactoryDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.MethodType,
                ObjectType.MethodType,
                ObjectType.MethodHandle,
                ObjectType.MethodType
            ),
            ObjectType.CallSite
        )
    }

    /**
     * Descriptor of the method `java.lang.invoke.LambdaMetafactory.altMetafactory`.
     */
    final val LambdaAltMetafactoryDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.MethodType,
                ArrayType.ArrayOfObject
            ),
            ObjectType.CallSite
        )
    }

    /**
     * Descriptor of the method `scala.runtime.LambdaDeserializer.bootstrap`.
     */
    final val ScalaLambdaDeserializeDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.MethodType,
                ArrayType.ArrayOfMethodHandle
            ),
            ObjectType.CallSite
        )
    }

    /**
     * Descriptor of the method `scala.runtime.SymbolLiteral.bootstrap`.
     */
    final val ScalaSymbolLiteralDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.MethodType,
                ObjectType.String
            ),
            ObjectType.CallSite
        )
    }

    /**
     * Descriptor of the method `scala.runtime.StructuralCallSite.bootstrap`.
     */
    final val ScalaStructuralCallSiteDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.MethodType,
                ObjectType.MethodType
            ),
            ObjectType.CallSite
        )
    }

    /**
     * Descriptor of the method `java.lang.invoke.ConstantBootstraps.primitiveClass`.
     */
    final val ConstantBootstrapsPrimitiveClassDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.MethodHandles$Lookup,
                ObjectType.String,
                ObjectType.Class
            ),
            ObjectType.Class
        )
    }

    /**
     * Descriptor of the methods `java.lang.invoke.MethodHandles$Lookup.findVarHandle` and
     * `java.lang.invoke.MethodHandles$Lookup.findStaticVarHandle`.
     */
    final val FindVarHandleDescriptor = {
        MethodDescriptor(
            ArraySeq(
                ObjectType.Class,
                ObjectType.String,
                ObjectType.Class
            ),
            ObjectType.VarHandle
        )
    }

    def withNoArgs(returnType: Type): MethodDescriptor = {
        (returnType.id: @scala.annotation.switch) match {
            case VoidType.id         => NoArgumentAndNoReturnValueMethodDescriptor
            case BooleanType.id      => JustReturnsBoolean
            case ByteType.id         => JustReturnsByte
            case ShortType.id        => JustReturnsShort
            case CharType.id         => JustReturnsChar
            case IntegerType.id      => JustReturnsInteger
            case LongType.id         => JustReturnsLong
            case FloatType.id        => JustReturnsFloat
            case DoubleType.id       => JustReturnsDouble
            case ObjectType.ObjectId => JustReturnsObject
            case ObjectType.StringId => JustReturnsString
            case ObjectType.ClassId  => JustReturnsClass
            case _                   => new NoArgumentMethodDescriptor(returnType)
        }
    }

    def apply(parameterType: FieldType, returnType: Type): MethodDescriptor = {
        new SingleArgumentMethodDescriptor(parameterType, returnType)
    }

    def apply(parameterTypes: FieldTypes, returnType: Type): MethodDescriptor = {
        (parameterTypes.size: @annotation.switch) match {
            case 0 =>
                withNoArgs(returnType)
            case 1 =>
                new SingleArgumentMethodDescriptor(parameterTypes(0), returnType)
            case 2 =>
                new TwoArgumentsMethodDescriptor(parameterTypes(0), parameterTypes(1), returnType)
            case _ =>
                new MultiArgumentsMethodDescriptor(parameterTypes, returnType)
        }
    }

    def apply(md: String): MethodDescriptor = {
        var index = 1 // we are not interested in the leading '('
        val parameterTypesBuilder = newFieldTypesBuilder()
        while (md.charAt(index) != ')') {
            val (ft, nextIndex) = parseParameterType(md, index)
            parameterTypesBuilder += ft
            index = nextIndex
        }

        val returnType = ReturnType(md.substring(index + 1))

        apply(parameterTypesBuilder.result(), returnType)
    }

    private[this] def parseParameterType(md: String, startIndex: Int): (FieldType, Int) = {
        val td = md.charAt(startIndex)
        (td: @scala.annotation.switch) match {
            case 'L' =>
                val endIndex = md.indexOf(';', startIndex + 1)
                ( // this is the return tuple
                    ObjectType(md.substring(startIndex + 1, endIndex)),
                    endIndex + 1
                )
            case '[' =>
                val (ft, index) = parseParameterType(md, startIndex + 1)
                ( // this is the return tuple
                    ArrayType(ft),
                    index
                )
            case _ =>
                ( // this is the return tuple
                    FieldType(td.toString),
                    startIndex + 1
                )
        }
    }
}

/**
 * Extractor for JVM method descriptors (for example, "(I[Ljava/lang/Object;])V").
 */
object JVMMethodDescriptor {

    def unapply(md: MethodDescriptor): Some[String] = Some(md.toJVMDescriptor)

}
