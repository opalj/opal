/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.CodeSequence

/**
 * Wrapper class to warp an array of statements.
 *
 * @author Michael Eichberg
 */
case class TACStmts[V <: Var[V]](
        instructions: Array[Stmt[V]]
) extends CodeSequence[Stmt[V]] {

    final override def pcOfPreviousInstruction(pc: Int): Int = {
        // The representation is compact: hence, the previous instruction/statement just
        // has the current index/pc - 1.
        pc - 1
    }

    final override def pcOfNextInstruction(pc: Int): Int = {
        // The representation is compact: hence, the previous instruction/statement just
        // has the current index/pc - 1.
        pc + 1
    }

}
