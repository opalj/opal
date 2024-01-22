/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGFactory

/**
 * Return from subroutine.
 *
 * @note A RET instruction always returns to an instruction immediately following a JSR(_W)
 *      instruction.
 *
 * @author Michael Eichberg
 */
case class RET(
    lvIndex: Int
) extends ControlTransferInstruction
    with ConstantLengthInstruction
    with NoLabels {

    override final def opcode: Opcode = RET.opcode

    override final def mnemonic: String = "ret"

    override final def isRET: Boolean = true

    override final def length: Int = 2

    override final def nextInstructions(
        currentPC:             Int /*PC*/,
        regularSuccessorsOnly: Boolean
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): List[Int /*PC*/ ] = {
        nextInstructions(currentPC, () => CFGFactory(code, classHierarchy))
    }

    override def jumpTargets(
        currentPC: Int
    )(
        implicit
        code:           Code,
        classHierarchy: ClassHierarchy = ClassHierarchy.PreInitializedClassHierarchy
    ): Iterator[PC] = {
        nextInstructions(currentPC, false /*irrelevant*/ ).iterator
    }

    final def nextInstructions(
        currentPC: Int,
        cfg:       () => CFG[Instruction, Code]
    )(
        implicit code: Code
    ): List[Int /*PC*/ ] = {

        // If we have just one subroutine it is sufficient to collect the
        // successor instructions of all JSR instructions.
        var jumpTargetPCs: List[Int] = List.empty
        code.iterate { (pc, instruction) =>
            if (pc != currentPC) { // filter this ret!
                instruction.opcode match {

                    case JSR.opcode | JSR_W.opcode =>
                        jumpTargetPCs ::= (instruction.indexOfNextInstruction(pc))

                    case RET.opcode =>
                        // we have found another RET ... hence, we have at least two subroutines
                        return cfg().successors(currentPC).toList;

                    case _ =>
                    // we don't care
                }
            }
        }
        jumpTargetPCs
    }

    override final def numberOfPoppedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    override final def numberOfPushedOperands(ctg: Int => ComputationalTypeCategory): Int = 0

    override final def stackSlotsChange: Int = 0

    override final def isIsomorphic(thisPC: Int, otherPC: Int)(implicit code: Code): Boolean = {
        this == code.instructions(otherPC)
    }

    override final def readsLocal: Boolean = true

    override final def indexOfReadLocal: Int = lvIndex

    override final def writesLocal: Boolean = false

    override final def indexOfWrittenLocal: Int = throw new UnsupportedOperationException()

    override final def toString(currentPC: Int): String = toString()
}

object RET extends InstructionMetaInformation {

    final val opcode = 169

}
