/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import org.opalj.br.Method

/**
 * Base interprocedural control flow graph for Java programs
 */
abstract class JavaBaseICFG extends JavaICFG {
    override def isNormalExitStatement(stmt: JavaStatement): Boolean = {
        stmt.index == stmt.basicBlock.asBasicBlock.endPC &&
        stmt.basicBlock.successors.exists(_.isNormalReturnExitNode)
    }

    override def isAbnormalExitStatement(stmt: JavaStatement): Boolean = {
        stmt.index == stmt.basicBlock.asBasicBlock.endPC &&
        stmt.basicBlock.successors.exists(_.isAbnormalReturnExitNode)
    }

    override def isCallStatement(stmt: JavaStatement): Boolean = {
        getCalleesIfCallStatement(stmt).nonEmpty
    }

    override def getCallable(stmt: JavaStatement): Method = stmt.method

    override def stringifyStatement(stmt: JavaStatement, indent: String = "", short: Boolean = false): String = {
        val stringifiedStatement = stmt.toString
        if (short) {
            stringifiedStatement.substring(0, stringifiedStatement.indexOf("{"))
        } else {
            stringifiedStatement
        }
    }
}
