/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package ifds

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.TACode

/**
 * An ICFG for a Java IFDS forward analysis.
 *
 * @param project the project to which the ICFG belongs.
 *
 * @author Nicolas Gross
 */
class JavaForwardICFG(project: SomeProject)
    extends JavaICFG(project) {

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
        statement.index == statement.basicBlock.asBasicBlock.endPC &&
        statement.basicBlock.successors.exists(_.isExitNode)
    }
}
