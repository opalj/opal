package org.opalj.tac2bc

import org.opalj.ba.CodeElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.{DUP, DUP2, RewriteLabel}
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{Assignment, Const, DVar, Expr, If, New, NewArray, Stmt, UVar, V, Var}
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
    private val usesLeft = mutable.Map[Int, Int]()

    /** Maps each variable to its definition index in TAC. */
    private val savedDefSites = mutable.Map[Var[V], IntTrieSet]()

    val visitedStmt: ArrayBuffer[Stmt[V]] = ArrayBuffer[Stmt[V]]()

    val delayedVisitStmt: ArrayBuffer[Stmt[V]] = ArrayBuffer[Stmt[V]]()

    def emitStmt(defIdx: Int,
                 delayStmtVisit: Boolean = false,
                 nestedStmt: Boolean = false): Unit = {
        val variable = getVarFromId(defIdx)
        val stmt = tacStmts(defIdx)._1
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)

        // Determine where this variable is used
        val usedIdx = variable match {
            case dvar: DVar[ValueInformation] => dvar.usedBy.head
            case uvar: UVar[ValueInformation] => uvar.definedBy.head
        }

        if (getDefSize(usedIdx, defIdx) > 1 || getUseSites(defIdx) > 1) {
            emitVarDef(variable)
        } else {
            val stmtIndex = tacStmts(defIdx)._2
            StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code, this, stmtIndex, delayStmtVisit, nestedStmt)
        }
    }

    def emitVarUse(variable: Var[V]): Unit = {
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)
        val defSites = getIndicesFromVariable(variable)
        val defIdx = defSites.head

        // Determine the First use-site index (where this variable is used)
        val usedIdx = variable match {
            case dvar: DVar[ValueInformation] => dvar.usedBy.head
            case uvar: UVar[ValueInformation] => uvar.definedBy.head
        }

        // If the current expression has multiple def-sites,
        // store the variable in a local to preserve its value.
        if(getDefSize(usedIdx, defIdx) > 1) {
            emitMultDef(variable, defSites)
        } else if (getUseSites(defIdx) > 1) {
            emitMultUse(variable, defIdx)
        } else {
            emitDef(defIdx)
        }
    }

    /**
     * Handles variables with multiple definition sites.
     */
    def emitVarDef(variable: Var[V]): Unit = {
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)
        ExprProcessor.loadVariable(variable, tacToLVIndex, code)
    }

    /**
     * Maintains essential tracking information for a variable.
     * Registers its definition sites in `savedDefSites` and initializes remaining use counts in `usesLeft`.
     */
    private def saveVariableInfo(variable: Var[V]): Unit = {
        val defSites = getIndicesFromVariable(variable)
        savedDefSites.getOrElseUpdate(variable, defSites)

        defSites.iterator.foreach(defIdx => usesLeft.getOrElseUpdate(defIdx, getUseSites(defIdx)))
    }

    /**
     * Emits store of variables in locals with multiple uses.
     */
    private def emitMultUse(variable: Var[V], defIdx: Int): Unit = {
        ExprProcessor.storeVariable(variable, tacToLVIndex, code)
        if (variable.cTpe.isCategory2) code += DUP2 else code += DUP
        emitDef(defIdx)
    }

    /**
     * Emits store of variables in locals with multiple definition sites.
     */
    private def emitMultDef(variable: Var[V], defSites: IntTrieSet): Unit = {
        ExprProcessor.storeVariable(variable, tacToLVIndex, code)

        defSites.iterator.foreach { defIdx =>
            emitDef(defIdx)
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
                    case newArray: NewArray[V] => ExprProcessor.processNewArray(newArray, tacToLVIndex, code, this)
                    case newExpr: New => ExprProcessor.processNewExpr(newExpr.tpe, code)
                    case _ =>
                }
            case _ =>
        }
    }

    /**
     * Returns all definition indices associated with the given variable.
     */
    private def getIndicesFromVariable(variable: Var[V]): IntTrieSet = {
        savedDefSites.getOrElseUpdate(
            variable, {
                variable match {
                    case dvar: DVar[ValueInformation] =>
                        IntTrieSet(dvar.originatedAt)

                    case uvar: UVar[ValueInformation] => uvar.definedBy
                    case _ =>
                        IntTrieSet.empty
                }
            }
        )
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
    private def getDefSize(useIdx: Int, defIdx: Int): Int = {
        tacStmts(useIdx)._1 match {
            case Assignment(_, _, expr) =>
                //muss für ein DVar auch gemacht werden
                findUVarInExpr(expr, defIdx)
                    .map(_.asVar.definedBy.size)
                    .getOrElse(0)
            case If(_, leftExpr, _, rightExpr, _) =>
                if(leftExpr.asVar.definedBy.contains(defIdx))
                    findUVarInExpr(leftExpr, defIdx)
                        .map(_.asVar.definedBy.size)
                        .getOrElse(0)
                else
                    findUVarInExpr(rightExpr, defIdx)
                        .map(_.asVar.definedBy.size)
                        .getOrElse(0)
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
    private def findUVarInExpr(expr: Expr[V], defIdx: Int): Option[UVar[_]] = {
        // First, check the root expression itself.
        var found: Option[UVar[_]] = expr match {
            case u: UVar[_] if u.definedBy.contains(defIdx) => return Some(u)
            case _ => None
        }

        // Run through all subexpressions.
        // It will stop traversal when the predicate returns false.
        expr.forallSubExpressions { sub =>
            sub match {
                case u: UVar[_] if u.definedBy.contains(defIdx) =>
                    found = Some(u)
                    false
                case _ =>
                    true
            }
        }
        found
    }
}
