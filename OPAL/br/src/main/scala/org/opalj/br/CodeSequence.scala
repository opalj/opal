/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A sequence of instructions; (currently) the common super trait of
 * [[org.opalj.tac.TACStmts]] and [[org.opalj.br.Code]])
 *
 * @tparam Instruction The type of instructions.
 *
 * @author Michael Eichberg
 */
trait CodeSequence[Instruction <: AnyRef] {

    def pcOfPreviousInstruction(pc: Int): Int /*PC*/

    def pcOfNextInstruction(pc: Int): Int /*PC*/

    def instructions: Array[Instruction]

}
