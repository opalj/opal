package org.opalj.tac2bc

import org.opalj.ba.CodeElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.{DUP, RewriteLabel}
import org.opalj.tac.{Assignment, Const, DVar, Stmt, V, Var}
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Context for translating TAC to bytecode in reverse order.
 */
class Tac2BcContext(
    tacStmts     : Array[(Stmt[V], Int)],
    tacToLVIndex : Map[Int,Int],
    code         : ListBuffer[CodeElement[Nothing]]
)(implicit project: SomeProject) {

    /** Remaining uses of a variable after its definition. */
    private val useSitesLeft = mutable.Map[Int, Int]()

    /** Maps each variable to its definition index in TAC. */
    private val savedDefSites = mutable.Map[Var[V], Int]()

    def emitStmt(defIdx: Int): Unit = {
        val variable = getVarFromId(defIdx)
        if (!savedDefSites.contains(variable)) {
            savedDefSites(variable) = defIdx
            useSitesLeft.getOrElseUpdate(defIdx, getUseSites(defIdx))
        }

        val stmt = tacStmts(defIdx)._1
        val labels = tacStmts.map(_ => RewriteLabel())
        StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code, this)
    }

    def emitVarUse(variable: Var[V]): Unit = {
        val defIdx = savedDefSites(variable)

        if(getUseSites(defIdx) > 1) {
            emitMultDef(variable, defIdx)
        }
        else {
            emitDef(defIdx)
        }
    }

    /**
     * Handles variables with multiple uses:
     * loads the value from a local onto the stack or stores it in a local.
     */
    private def emitMultDef(variable: Var[V], defIdx: Int): Unit = {
        useSitesLeft(defIdx) -= 1

        if(useSitesLeft(defIdx) == 0) {
            ExprProcessor.storeVariable(variable, tacToLVIndex, code)
            code += DUP
            emitDef(defIdx)
        } else {
            ExprProcessor.loadVariable(variable, tacToLVIndex, code)
        }
    }

    /**
     * Emits bytecode for the definition of a variable at the given index.
     * Loads a constant onto the stack.
     */
    private def emitDef(defIdx: Int): Unit = {
        val stmt = tacStmts(defIdx)._1
        stmt match {
            case Assignment(_, _, expr) =>
                expr match {
                    case const: Const => ExprProcessor.loadConstant(const, code)
                }
            case _ =>
        }
    }

    /**
     * Returns the number of use-sites for a variable at the given definition index.
     */
    private def getUseSites(defIdx: Int): Int = {
        tacStmts(defIdx)._1 match {
            case Assignment(_, dvar: DVar[ValueInformation], _) => dvar.usedBy.size
            case _ => throw new NoSuchElementException("There are no variables in Statements.")
        }
    }

    /**
     * Returns the variable corresponding to the given definition index.
     */
    private def getVarFromId(defIdx: Int): Var[V] = {
        tacStmts(defIdx)._1 match {
            case Assignment(_, dvar: DVar[ValueInformation], _) => dvar.asVar
            case _ => throw new NoSuchElementException("There are no variables in Statements.")
        }
    }
}
