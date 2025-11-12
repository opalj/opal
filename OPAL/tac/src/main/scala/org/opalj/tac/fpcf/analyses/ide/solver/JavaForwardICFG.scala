/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package solver

import scala.collection.mutable.Set as MutableSet

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Interprocedural control flow graph for Java programs in forward direction. This implementation is based on the
 * [[org.opalj.tac.fpcf.analyses.ifds.JavaForwardICFG]] from IFDS.
 *
 * @author Robin Körkemeier
 */
class JavaForwardICFG(project: SomeProject) extends JavaBaseICFG(project) {
    override def getStartStatements(callable: Method): Set[JavaStatement] = {
        val tac = tacProvider(callable)
        Set(
            JavaStatement(callable, 0, isReturnNode = false, tac.stmts, tac.cfg)
        )
    }

    override def getNextStatements(javaStmt: JavaStatement): Set[JavaStatement] = {
        if (isCallStatement(javaStmt)) {
            Set(
                JavaStatement(javaStmt.method, javaStmt.tacIndex, isReturnNode = true, javaStmt.stmts, javaStmt.cfg)
            )
        } else {
            val successors = MutableSet.empty[JavaStatement]
            javaStmt.cfg.foreachSuccessor(javaStmt.tacIndex) { nextPc =>
                successors.add(
                    JavaStatement(javaStmt.method, nextPc, isReturnNode = false, javaStmt.stmts, javaStmt.cfg)
                )
            }
            successors.toSet
        }
    }

    override def isNormalExitStatement(javaStmt: JavaStatement): Boolean = {
        javaStmt.tacIndex == javaStmt.basicBlock.asBasicBlock.endPC &&
        javaStmt.basicBlock.successors.exists(_.isNormalReturnExitNode)
    }

    override def isAbnormalExitStatement(javaStmt: JavaStatement): Boolean = {
        javaStmt.tacIndex == javaStmt.basicBlock.asBasicBlock.endPC &&
        javaStmt.basicBlock.successors.exists(_.isAbnormalReturnExitNode)
    }
}
