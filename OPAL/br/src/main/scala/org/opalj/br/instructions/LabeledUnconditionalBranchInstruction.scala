/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Super class of all labeled bytecode instructions that always jump to a specific
 * target instruction.
 *
 * @author Malte Limmeroth
 */
trait LabeledUnconditionalBranchInstruction
    extends LabeledSingleJumpTargetInstruction
    with UnconditionalBranchInstructionLike {

    override def toString(currentPC: Int): String = getClass.getSimpleName+" "+branchTarget
}
