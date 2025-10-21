package org.opalj.tac2bc

import org.opalj.ba.CodeElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.{DUP, DUP2, RewriteLabel}
import org.opalj.tac.{Assignment, Const, DVar, Expr, NewArray, Stmt, UVar, V, Var, New}
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * Context for translating TAC to bytecode in reverse order.
 */
class Tac2BcContext(
    tacStmts     : Array[(Stmt[V], Int)],
    tacToLVIndex : Map[Int,Int],
    code         : ListBuffer[CodeElement[Nothing]],
    labels       : Array[RewriteLabel]
)(implicit project: SomeProject) {

    /** Remaining uses of a variable after its definition. */
    private val useSitesLeft = mutable.Map[Int, Int]()

    /** Maps each variable to its definition index in TAC. */
    private val savedDefSites = mutable.Map[Var[V], Int]()

    val visitedStmt: ArrayBuffer[Stmt[V]] = ArrayBuffer[Stmt[V]]()

    val delayedVisitStmt: ArrayBuffer[Stmt[V]] = ArrayBuffer[Stmt[V]]()

    def emitStmt(defIdx: Int,
                 delayStmtVisit: Boolean = false): Unit = {
        val variable = getVarFromId(defIdx)
        val stmt = tacStmts(defIdx)._1
        if (!savedDefSites.contains(variable)) {
            savedDefSites(variable) = defIdx
            useSitesLeft.getOrElseUpdate(defIdx, getUseSites(defIdx))
        }

        val stmtIndex = tacStmts(defIdx)._2
        StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code, this, stmtIndex, delayStmtVisit)
    }

    def emitVarUse(variable: Var[V]): Unit = {
        // Determine the definition index for this variable (and cache it if not known yet).
        val defIdx = savedDefSites.get(variable) match {
            case Some(idx) => idx
            case None =>
                val idx = variable match {
                    case dvar: DVar[ValueInformation] => dvar.originatedAt
                    case uvar: UVar[ValueInformation] => uvar.definedBy.head
                }
                savedDefSites(variable) = idx
                idx
        }

        // Determine the First use-site index (where this variable is used)
        val usedIdx = variable match {
            case dvar: DVar[ValueInformation] => dvar.usedBy.head
        }

        // If the current expression has multiple def-sites,
        // store the variable in a local to preserve its value.
        if(getDefSites(usedIdx) > 1) {
            ExprProcessor.storeVariable(variable, tacToLVIndex, code)
        }

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
            if (variable.cTpe.isCategory2) code += DUP2 else code += DUP
            emitDef(defIdx)
        } else {
            ExprProcessor.loadVariable(variable, tacToLVIndex, code)
        }
    }

    /**
     * Emits bytecode for the definition of a variable at the given index.
     * Loads an end node to the stack.
     */
    private def emitDef(defIdx: Int): Unit = {
        val stmt = tacStmts(defIdx)._1
        stmt match {
            case Assignment(_, _, expr) =>
                expr match {
                    case const: Const => ExprProcessor.loadConstant(const, code)
                    case newExpr: New => ExprProcessor.processNewExpr(newExpr.tpe, code)
                    case newArray: NewArray[V] => ExprProcessor.processNewArray(newArray, tacToLVIndex, code, this)
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
     * Returns the number of def-sites for the variable used inside the expression
     * of the statement at the given definition index.
     */
    private def getDefSites(defIdx: Int): Int = {
        tacStmts(defIdx)._1 match {
            case Assignment(_, _, expr) =>
                findUVarInExpr(expr)
                    .map(_.asVar.definedBy.size)
                    .getOrElse(throw new NoSuchElementException("No UVar in given expression."))
            case _ => 0
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

    /** Checks if a TAC statement has already been visited. */
    def isStmtVisited(stmt: Stmt[V]): Boolean = {
        visitedStmt.contains(stmt)
    }

    /** Checks if a TAC statement has been delayed. */
    def isStmtVisitDelayed(stmt: Stmt[V]): Boolean = {
        delayedVisitStmt.contains(stmt)
    }

    /**
     * Returns the given expression and returns the first UVar found, if any.
     */
    private def findUVarInExpr(expr: Expr[V]): Option[UVar[_]] = {
        // First, check the root expression itself.
        var found: Option[UVar[_]] = expr match {
            case u: UVar[_] => Some(u)
            case _          => None
        }

        // Run through all subexpressions.
        // It will stop traversal when the predicate returns false.
        expr.forallSubExpressions { sub =>
            sub match {
                case u: UVar[_] =>
                    found = Some(u)
                    false
                case _ =>
                    true
            }
        }
        found
    }

    /**
     * Adjusts the use count of the array reference used by an ArrayLoad.
     */
    def increaseUseSitesForArrRef(arrLoadVar: Var[V], arrRefDefIdx: Int): Unit = {
        if (!useSitesLeft.contains(arrRefDefIdx)) {
            val arrRefUseSites = getUseSites(arrRefDefIdx)
            val arrLoadVarUseSites = arrLoadVar.asVar.usedBy.size
            val newUseSites = arrLoadVarUseSites + arrRefUseSites
            useSitesLeft.getOrElseUpdate(arrRefDefIdx, newUseSites - 1)
        }
    }
}
