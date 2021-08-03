/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Push item from runtime constant pool.
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
sealed trait LDC[@specialized(Int, Float) T]
    extends LoadConstantInstruction[T]
    with InstructionMetaInformation {

    final def opcode: Opcode = LDC.opcode

    final def mnemonic: String = "ldc"

    final def length: Int = 2

    def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || (
            this.opcode == other.opcode &&
            this.value == other.asInstanceOf[LDC[_]].value
        )
    }
}

/** A load constant instruction which never fails. */
sealed abstract class PrimitiveLDC[@specialized(Int, Float) T] extends LDC[T]

/**
 * @note To match [[LoadInt]] and [[LoadInt_W]] instructions you can use [[LDCInt]].
 */
final case class LoadInt(value: Int) extends PrimitiveLDC[Int] {

    final def computationalType = ComputationalTypeInt

}

/**
 * @note To match [[LoadFloat]] and [[LoadFloat_W]] instructions you can use [[LDCFloat]].
 */
final case class LoadFloat(value: Float) extends PrimitiveLDC[Float] {

    final def computationalType = ComputationalTypeFloat

    override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        this.similar(other)
    }

    override def similar(other: Instruction): Boolean = {
        LDC.opcode == other.opcode && other.isInstanceOf[LoadFloat] && {
            val otherLoadFloat = other.asInstanceOf[LoadFloat]
            (this.value.isNaN && otherLoadFloat.value.isNaN) ||
                (this.value == otherLoadFloat.value)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case LoadFloat(thatValue) =>
                thatValue == this.value || (thatValue.isNaN && this.value.isNaN)
            case _ => false
        }
    }

    // HashCode of "value.NaN" is stable and 0
}

/**
 * @note To match [[LoadClass]] and [[LoadClass_W]] instructions you can use [[LDCClass]].
 */
final case class LoadClass(value: ReferenceType) extends LDC[ReferenceType] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadMethodHandle]] and [[LoadMethodHandle_W]] instructions you
 * can use [[LDCMethodHandle]].
 */
final case class LoadMethodHandle(value: MethodHandle) extends LDC[MethodHandle] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadMethodType]] and [[LoadMethodType_W]] instructions you can use
 * [[LDCMethodType]].
 */
final case class LoadMethodType(value: MethodDescriptor) extends LDC[MethodDescriptor] {
    final def computationalType = ComputationalTypeReference
}

/**
 * @note To match [[LoadString]] and [[LoadString_W]] instructions you can use [[LDCString]].
 */
final case class LoadString(value: String) extends PrimitiveLDC[String] {

    final def computationalType = ComputationalTypeReference

    override def toString: String = "LoadString(\""+value+"\")"

}

/**
 * @note To match [[LoadDynamic]], [[LoadDynamic_W]] and [[LoadDynamic2_W]] instructions you can use
 *       [[LDCDynamic]].
 */
final case class LoadDynamic(
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      FieldType
) extends LDC[Nothing] {
    def value: Nothing = throw new UnsupportedOperationException("dynamic constant unknown")

    def computationalType: ComputationalType = descriptor.computationalType

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = {
        val other = code.instructions(otherPC)
        (this eq other) || this == other
    }
}

case object INCOMPLETE_LDC extends LDC[Any] {

    private def error: Nothing = {
        val message = "this ldc is incomplete"
        throw BytecodeProcessingFailedException(message)
    }

    final def computationalType = error

    final def value: Any = error

    final override def isIsomorphic(thisPC: PC, otherPC: PC)(implicit code: Code): Boolean = error
}

/**
 * Defines factory and extractor methods for LDC instructions.
 *
 * @author Michael Eichberg
 */
object LDC {

    def apply(constantValue: ConstantValue[_]): LDC[_] = {
        constantValue.value match {
            case i: Int               => LoadInt(i)
            case f: Float             => LoadFloat(f)
            case r: ReferenceType     => LoadClass(r)
            case s: String            => LoadString(s)
            case mh: MethodHandle     => LoadMethodHandle(mh)
            case md: MethodDescriptor => LoadMethodType(md)
            case _ =>
                throw BytecodeProcessingFailedException(
                    "unsupported constant value: "+constantValue
                )
        }
    }

    def unapply[T](ldc: LDC[T]): Option[T] = Some(ldc.value)

    final val opcode = 18

}
