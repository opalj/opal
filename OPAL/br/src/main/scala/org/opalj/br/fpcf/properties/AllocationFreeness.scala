/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import scala.annotation.switch

import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.OrderedProperty
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.ASTORE_0
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.PUTFIELD

sealed trait AllocationFreenessPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = AllocationFreeness

}

/**
 * Describes whether a [[org.opalj.br.DeclaredMethod]] or its (transitive) callees may allocate any
 * objects/arrays.
 *
 * @author Dominik Helm
 */
sealed abstract class AllocationFreeness
    extends OrderedProperty
    with IndividualProperty[AllocationFreeness, VirtualMethodAllocationFreeness]
    with AllocationFreenessPropertyMetaInformation {

    /**
     * The globally unique key of the [[AllocationFreeness]] property.
     */
    final def key: PropertyKey[AllocationFreeness] = AllocationFreeness.key

    final val aggregatedProperty = new VirtualMethodAllocationFreeness(this)
}

object AllocationFreeness extends AllocationFreenessPropertyMetaInformation {
    /**
     * The key associated with every allocation freeness property. The name is "AllocationFreeness";
     * the fallback is "MethodWithAllocations".
     */
    final val key = PropertyKey.create[DeclaredMethod, AllocationFreeness](
        "AllocationFreeness",
        (_: PropertyStore, _: FallbackReason, dm: DeclaredMethod) => {
            if (dm.hasSingleDefinedMethod && dm.definedMethod.body.isDefined) {
                val method = dm.definedMethod
                val body = method.body.get
                val instructions = body.instructions
                val maxPC = instructions.length

                var overwritesSelf = false
                var mayOverwriteSelf = true
                var hasAllocation = false

                var currentPC = 0
                while (currentPC < maxPC && !hasAllocation) {
                    val instruction = instructions(currentPC)
                    (instruction.opcode: @switch) match {
                        case NEW.opcode | NEWARRAY.opcode |
                            ANEWARRAY.opcode | MULTIANEWARRAY.opcode =>
                            hasAllocation = true
                        case INVOKESTATIC.opcode | INVOKESPECIAL.opcode | INVOKEVIRTUAL.opcode |
                            INVOKEINTERFACE.opcode | INVOKEDYNAMIC.opcode =>
                            hasAllocation = true
                        case ASTORE_0.opcode if !method.isStatic =>
                            if (mayOverwriteSelf) overwritesSelf = true
                            else hasAllocation = true
                        case PUTFIELD.opcode | GETFIELD.opcode => // may allocate NPE on non-receiver
                            if (method.isStatic || overwritesSelf)
                                hasAllocation = true
                            else if (instructions(body.pcOfPreviousInstruction(currentPC)).opcode !=
                                ALOAD_0.opcode)
                                hasAllocation = true
                            else
                                mayOverwriteSelf = false
                        case _ => hasAllocation = instruction.jvmExceptions.nonEmpty
                    }
                    currentPC = body.pcOfNextInstruction(currentPC)
                }
                if (hasAllocation) MethodWithAllocations else AllocationFreeMethod
            } else MethodWithAllocations
        }
    )
}

/**
 * The method does not allocate new objects/arrays and neither does any of its (transitive) callees.
 */
case object AllocationFreeMethod extends AllocationFreeness {

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationFreeness): Unit = {}

    override def meet(other: AllocationFreeness): AllocationFreeness = other
}

/**
 * The method or any of its (transitive) callees may allocate new objects/arrays.
 */
case object MethodWithAllocations extends AllocationFreeness {

    override def checkIsEqualOrBetterThan(e: Entity, other: AllocationFreeness): Unit = {
        if (other ne MethodWithAllocations)
            throw new IllegalArgumentException(s"$e: impossible refinement: $other => $this")
    }

    override def meet(other: AllocationFreeness): AllocationFreeness = this
}
