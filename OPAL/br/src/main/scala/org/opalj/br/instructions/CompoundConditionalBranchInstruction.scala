/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.Chain

/**
 * Common super class of all compound conditional branch instructions
 * (switch instructions!).
 *
 * @author Michael Eichberg
 */
trait CompoundConditionalBranchInstructionLike extends ConditionalBranchInstructionLike {

    override def operandCount: Int = 1

    final override def stackSlotsChange: Int = -1

    /**
     * Returns all case values that are '''not related to the default branch'''.
     */
    def caseValues: IntIterator

}

trait CompoundConditionalBranchInstruction
    extends ConditionalBranchInstruction
    with CompoundConditionalBranchInstructionLike {

    final override def isCompoundConditionalBranchInstruction: Boolean = true
    final override def asCompoundConditionalBranchInstruction: this.type = this

    def defaultOffset: Int

    def jumpOffsets: Iterable[Int]

    final override def jumpTargets(
        currentPC: PC
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        jumpOffsets.iterator.map(_ + currentPC) ++ Iterator(defaultOffset + currentPC)
    }

    /**
     * Returns the case value(s) that are associated with the given `jumpOffset`.
     * If the `jumpOffset` is also the `defaultOffset`, the return value's second
     * value is true.
     */
    def caseValueOfJumpOffset(jumpOffset: Int): (Chain[Int], Boolean)

}
