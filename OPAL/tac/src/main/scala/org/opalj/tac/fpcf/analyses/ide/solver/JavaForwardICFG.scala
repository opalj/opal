/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package solver

import scala.collection
import scala.collection.immutable
import scala.collection.mutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Interprocedural control flow graph for Java programs in forward direction. This implementation is based on the
 * [[org.opalj.tac.fpcf.analyses.ifds.JavaForwardICFG]] from IFDS.
 *
 * @author Robin Körkemeier
 */
class JavaForwardICFG(project: SomeProject) extends JavaBaseICFG(project) {
    override def getStartStatements(callable: Method): collection.Set[JavaStatement] = {
        val tac = tacProvider(callable)
        immutable.Set(
            JavaStatement(callable, 0, isReturnNode = false, tac.stmts, tac.cfg)
        )
    }

    override def getNextStatements(javaStmt: JavaStatement): collection.Set[JavaStatement] = {
        if (isCallStatement(javaStmt)) {
            immutable.Set(
                JavaStatement(javaStmt.method, javaStmt.pc, isReturnNode = true, javaStmt.stmts, javaStmt.cfg)
            )
        } else {
            val successors = mutable.Set.empty[JavaStatement]
            javaStmt.cfg.foreachSuccessor(javaStmt.pc) { nextPc =>
                successors.add(
                    JavaStatement(javaStmt.method, nextPc, isReturnNode = false, javaStmt.stmts, javaStmt.cfg)
                )
            }
            successors
        }
    }

    override def isNormalExitStatement(javaStmt: JavaStatement): Boolean = {
        javaStmt.pc == javaStmt.basicBlock.asBasicBlock.endPC &&
        javaStmt.basicBlock.successors.exists(_.isNormalReturnExitNode)
    }

    override def isAbnormalExitStatement(javaStmt: JavaStatement): Boolean = {
        javaStmt.pc == javaStmt.basicBlock.asBasicBlock.endPC &&
        javaStmt.basicBlock.successors.exists(_.isAbnormalReturnExitNode)
    }
}
