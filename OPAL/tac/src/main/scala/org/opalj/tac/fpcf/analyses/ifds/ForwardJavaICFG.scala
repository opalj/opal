/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.{DUVar, LazyDetachedTACAIKey, TACMethodParameter, TACode}
import org.opalj.value.ValueInformation

class ForwardJavaICFG(implicit project: SomeProject)
    extends JavaICFG {

    val tacai: Method => TACode[TACMethodParameter, DUVar[ValueInformation]] = project.get(LazyDetachedTACAIKey)

    /**
     * Determines the statements at which the analysis starts.
     *
     * @param callable The analyzed callable.
     * @return The statements at which the analysis starts.
     */
    override def startStatements(callable: Method): Set[JavaStatement] = {
        val TACode(_, code, _, cfg, _) = tacai(callable)
        Set(JavaStatement(callable, 0, code, cfg))
    }

    /**
     * Determines the statement, that will be analyzed after some other `statement`.
     *
     * @param statement The source statement.
     * @return The successor statements
     */
    override def nextStatements(statement: JavaStatement): Set[JavaStatement] = {
        statement.cfg
            .successors(statement.index)
            .map { index => JavaStatement(statement, index) }
    }

    /**
     * Determines whether the statement is an exit statement.
     *
     * @param statement The source statement.
     * @return Whether the statement flow may exit its callable (function/method)
     */
    override def isExitStatement(statement: JavaStatement): Boolean = {
        statement.index == statement.node.asBasicBlock.endPC &&
            statement.node.successors.exists(_.isExitNode)
    }
}
