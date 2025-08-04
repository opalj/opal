/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package solver

import scala.collection.mutable.{Set => MutableSet}

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Interprocedural control flow graph for Java programs in backward direction. This implementation is based on the
 * [[org.opalj.tac.fpcf.analyses.ifds.JavaBackwardICFG]] from IFDS.
 *
 * @author Robin Körkemeier
 */
class JavaBackwardICFG(project: SomeProject) extends JavaBaseICFG(project) {
    override def getStartStatements(callable: Method): Set[JavaStatement] = {
        val tac = tacProvider(callable)
        (tac.cfg.normalReturnNode.predecessors ++ tac.cfg.abnormalReturnNode.predecessors)
            .map { node => JavaStatement(callable, node.asBasicBlock.endPC, isReturnNode = false, tac.stmts, tac.cfg) }
    }

    override def getNextStatements(javaStmt: JavaStatement): Set[JavaStatement] = {
        if (isCallStatement(javaStmt)) {
            Set(
                JavaStatement(javaStmt.method, javaStmt.pc, isReturnNode = true, javaStmt.stmts, javaStmt.cfg)
            )
        } else {
            val predecessors = MutableSet.empty[JavaStatement]
            javaStmt.cfg.foreachPredecessor(javaStmt.pc) { prevPc =>
                predecessors.add(
                    JavaStatement(javaStmt.method, prevPc, isReturnNode = false, javaStmt.stmts, javaStmt.cfg)
                )
            }
            predecessors.toSet
        }
    }

    override def isNormalExitStatement(stmt: JavaStatement): Boolean = {
        stmt.pc == 0
    }

    override def isAbnormalExitStatement(stmt: JavaStatement): Boolean = {
        false
    }
}
