/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Puts a constant value on the stack.
 *
 * @author Michael Eichberg
 */
abstract class LoadConstantInstruction[T]
    extends Instruction
    with ConstantLengthInstruction
    with NoLabels {

    override def isLoadConstantInstruction: Boolean = true

    /**
     * The value that is put onto the stack.
     */
    def value: T

    /**
     * Returns the computational type category of the pushed value.
     */
    def computationalType: ComputationalType

    final def jvmExceptions: List[ObjectType] = Nil

    final def mayThrowExceptions: Boolean = false

    final def nextInstructions(
        currentPC:             PC,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[PC] = {
        List(indexOfNextInstruction(currentPC))
    }

    def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 1

    final def stackSlotsChange: Int = +computationalType.operandSize

    def readsLocal: Boolean = false

    @throws[UnsupportedOperationException]("always")
    def indexOfReadLocal: Int = throw new UnsupportedOperationException()

    def writesLocal: Boolean = false

    @throws[UnsupportedOperationException]("always")
    def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    final def expressionResult: Stack.type = Stack

    final override def toString(currentPC: Int): String = toString()
}
/**
 * Defines factory methods for `LoadConstantInstruction`s.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
object LoadConstantInstruction {

    /**
     * Returns the instruction that puts the given constant integer value on top of the
     * stack.
     */
    def apply(i: Int): LoadConstantInstruction[Int] =
        (i: @scala.annotation.switch) match {
            case -1                                              => ICONST_M1
            case 0                                               => ICONST_0
            case 1                                               => ICONST_1
            case 2                                               => ICONST_2
            case 3                                               => ICONST_3
            case 4                                               => ICONST_4
            case 5                                               => ICONST_5
            case _ if i >= Byte.MinValue && i <= Byte.MaxValue   => BIPUSH(i)
            case _ if i >= Short.MinValue && i <= Short.MaxValue => SIPUSH(i)
            case _                                               => LoadInt(i)
        }

    def apply(c: ConstantValue[_], wide: Boolean): LoadConstantInstruction[_] =
        c match {
            case ConstantDouble(d)    => LoadDouble(d)
            case ConstantFloat(f)     => if (wide) LoadFloat_W(f) else LoadFloat(f)
            case ConstantInteger(i)   => if (wide) LoadInt_W(i) else LoadInt(i)
            case ConstantLong(l)      => LoadLong(l)
            case ConstantString(s)    => if (wide) LoadString_W(s) else LoadString(s)
            case ConstantClass(c)     => if (wide) LoadClass_W(c) else LoadClass(c)
            case md: MethodDescriptor => if (wide) LoadMethodType_W(md) else LoadMethodType(md)
            case mh: MethodHandle     => if (wide) LoadMethodHandle_W(mh) else LoadMethodHandle(mh)
            case DynamicConstant(bootstrapMethod, name, descriptor) =>
                if (descriptor.computationalType.isCategory2)
                    LoadDynamic2_W(bootstrapMethod, name, descriptor)
                else if (wide)
                    LoadDynamic_W(bootstrapMethod, name, descriptor)
                else
                    LoadDynamic(bootstrapMethod, name, descriptor)
        }

    def unapply[T](ldc: LoadConstantInstruction[T]): Some[T] = Some(ldc.value)

    /**
     * Returns the instruction that puts the constant value on top of the stack
     * that represents the default value that is used to initialize fields
     * of the corresponding type.
     */
    def defaultValue(fieldType: FieldType): LoadConstantInstruction[_] =
        (fieldType.id: @scala.annotation.switch) match {
            case IntegerType.id => ICONST_0
            case ByteType.id    => ICONST_0
            case CharType.id    => ICONST_0
            case ShortType.id   => ICONST_0
            case BooleanType.id => ICONST_0
            case LongType.id    => LCONST_0
            case FloatType.id   => FCONST_0
            case DoubleType.id  => DCONST_0
            case _              => ACONST_NULL
        }
}

object LDCFloat {

    def unapply(ldc: LoadConstantInstruction[_]): Option[Float] = {
        ldc match {
            case LoadFloat(value)   => Some(value)
            case LoadFloat_W(value) => Some(value)
            case _                  => None
        }
    }
}

object LDCInt {

    def unapply(ldc: LoadConstantInstruction[_]): Option[Int] = {
        ldc match {
            case LoadInt(value)   => Some(value)
            case LoadInt_W(value) => Some(value)
            case _                => None
        }
    }
}

object LDCClass {

    def unapply(ldc: LoadConstantInstruction[_]): Option[ReferenceType] = {
        ldc match {
            case LoadClass(value)   => Some(value)
            case LoadClass_W(value) => Some(value)
            case _                  => None
        }
    }
}

object LDCString {

    def unapply(ldc: LoadConstantInstruction[_]): Option[String] = {
        ldc match {
            case LoadString(value)   => Some(value)
            case LoadString_W(value) => Some(value)
            case _                   => None
        }
    }
}

object LDCMethodHandle {

    def unapply(ldc: LoadConstantInstruction[_]): Option[MethodHandle] = {
        ldc match {
            case LoadMethodHandle(value)   => Some(value)
            case LoadMethodHandle_W(value) => Some(value)
            case _                         => None
        }
    }
}

object LDCMethodType {

    def unapply(ldc: LoadConstantInstruction[_]): Option[MethodDescriptor] = {
        ldc match {
            case LoadMethodType(value)   => Some(value)
            case LoadMethodType_W(value) => Some(value)
            case _                       => None
        }
    }
}

object LDCDynamic {

    def unapply(ldc: LoadConstantInstruction[_]): Option[(BootstrapMethod, String, FieldType)] = {
        ldc match {
            case LoadDynamic(bootstrapMethod, name, descriptor) =>
                Some((bootstrapMethod, name, descriptor))
            case LoadDynamic_W(bootstrapMethod, name, descriptor) =>
                Some((bootstrapMethod, name, descriptor))
            case LoadDynamic2_W(bootstrapMethod, name, descriptor) =>
                Some((bootstrapMethod, name, descriptor))
            case _ =>
                None
        }
    }
}
