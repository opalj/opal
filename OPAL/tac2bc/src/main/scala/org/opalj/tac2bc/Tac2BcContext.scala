package org.opalj.tac2bc

import org.opalj.ba.CodeElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.RewriteLabel
import org.opalj.tac.{ Stmt, V}

import scala.collection.mutable.ListBuffer

class Tac2BcContext(
    tacStmts     : Array[(Stmt[V], Int)],
    tacToLVIndex : Map[Int,Int],
    code         : ListBuffer[CodeElement[Nothing]]
)(implicit project: SomeProject) {

    private val visitedStmts = Array.fill(tacStmts.length)(false)

    def emitStmt(idx: Int): Unit = {
        if (idx < 0 || visitedStmts(idx)) return
        visitedStmts(idx) = true

        val stmt = tacStmts(idx)._1
        val labels = tacStmts.map(_ => RewriteLabel())
        StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code, this)
    }
}
