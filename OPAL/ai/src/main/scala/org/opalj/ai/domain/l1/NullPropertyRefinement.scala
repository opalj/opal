/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import scala.annotation.switch

import org.opalj.value.TypeOfReferenceValue
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.VirtualMethodInvocationInstruction
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.BALOAD
import org.opalj.br.instructions.CALOAD
import org.opalj.br.instructions.SALOAD
import org.opalj.br.instructions.IALOAD
import org.opalj.br.instructions.LALOAD
import org.opalj.br.instructions.FALOAD
import org.opalj.br.instructions.DALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.BASTORE
import org.opalj.br.instructions.CASTORE
import org.opalj.br.instructions.SASTORE
import org.opalj.br.instructions.IASTORE
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.FASTORE
import org.opalj.br.instructions.DASTORE
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.Instruction
import org.opalj.br.ObjectType

/**
 * Refines a reference's null property if the reference value may be null and
 * this has resulted in a corresponding exception.
 *
 * @author Michael Eichberg
 */
trait NullPropertyRefinement extends CoreDomainFunctionality {
    domain: ReferenceValuesDomain with Origin =>

    abstract override def afterEvaluation(
        pc:                       Int,
        instruction:              Instruction,
        oldOperands:              Operands,
        oldLocals:                Locals,
        targetPC:                 Int,
        isExceptionalControlFlow: Boolean,
        forceJoin:                Boolean,
        newOperands:              Operands,
        newLocals:                Locals
    ): (Operands, Locals) = {

        @inline def default() =
            super.afterEvaluation(
                pc, instruction, oldOperands, oldLocals,
                targetPC, isExceptionalControlFlow, forceJoin, newOperands, newLocals
            )

        def establishNullProperty(objectRef: DomainValue): (Operands, Locals) = {
            if (refIsNull(pc, objectRef).isUnknown) {
                if (isExceptionalControlFlow && {
                    // the NullPointerException was created by the JVM, because
                    // the objectRef is (assumed to be) null
                    val exception = newOperands.head
                    val TypeOfReferenceValue(utb) = exception
                    (utb.head eq ObjectType.NullPointerException) && {
                        val origins = originsIterator(exception)
                        origins.nonEmpty && {
                            val origin = origins.next()
                            isImmediateVMException(origin) && pcOfImmediateVMException(origin) == pc &&
                                !origins.hasNext
                        }
                    }
                }) {
                    val (operands2, locals2) =
                        refEstablishIsNull(targetPC, objectRef, newOperands, newLocals)
                    super.afterEvaluation(
                        pc, instruction, oldOperands, oldLocals,
                        targetPC, isExceptionalControlFlow, forceJoin, operands2, locals2
                    )
                } else {
                    // ... the value is not null... even if an exception was thrown,
                    // because the exception is not a VM-level `NullPointerException`
                    val (operands2, locals2) =
                        refEstablishIsNonNull(targetPC, objectRef, newOperands, newLocals)
                    super.afterEvaluation(
                        pc, instruction, oldOperands, oldLocals,
                        targetPC, isExceptionalControlFlow, forceJoin, operands2, locals2
                    )
                }
            } else {
                default()
            }
        }

        (instruction.opcode: @switch) match {
            case AALOAD.opcode
                | BALOAD.opcode
                | CALOAD.opcode
                | SALOAD.opcode
                | IALOAD.opcode
                | LALOAD.opcode
                | FALOAD.opcode
                | DALOAD.opcode =>
                val arrayRef = oldOperands(1)
                establishNullProperty(arrayRef)

            case AASTORE.opcode
                | BASTORE.opcode
                | CASTORE.opcode
                | SASTORE.opcode
                | IASTORE.opcode
                | LASTORE.opcode
                | FASTORE.opcode
                | DASTORE.opcode =>
                val arrayRef = oldOperands(2)
                establishNullProperty(arrayRef)

            case ARRAYLENGTH.opcode =>
                val arrayRef = oldOperands.head
                establishNullProperty(arrayRef)

            case MONITORENTER.opcode /* not necessary: | MONITOREXIT.opcode (every monitorexit is preceeded by a monitorenter) */ =>
                val monitor = oldOperands.head
                establishNullProperty(monitor)

            case GETFIELD.opcode =>
                val objectRef = oldOperands.head
                establishNullProperty(objectRef)

            case PUTFIELD.opcode =>
                val objectRef = oldOperands.tail.head
                establishNullProperty(objectRef)

            // THE RECEIVER OF AN INVOKESPECIAL IS ALWAYS "THIS" AND, HENCE, IS IRRELEVANT!
            case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode =>
                val invoke = instruction.asInstanceOf[VirtualMethodInvocationInstruction]
                val receiver = oldOperands(invoke.methodDescriptor.parametersCount)
                establishNullProperty(receiver)

            case _ =>
                default()
        }
    }
}
