package org.opalj.tac2bc

import org.opalj.ba.CodeElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.RewriteLabel
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{ArrayLoad, Assignment, BinaryExpr, Call, Const, DVar, Expr, ExprStmt, GetStatic, If, New, NewArray, Stmt, Switch, UVar, V, Var}
import org.opalj.tac2bc.ExprProcessor.{processArrayLoad, processBinaryExpr, processCall}
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

    /** Variables that have been saved to local variables. */
    val loadedStmt: ArrayBuffer[Var[V]] = ArrayBuffer[Var[V]]()

    /** Statements that have been marked as end nodes. */
    val endNodes: ArrayBuffer[Stmt[V]] = ArrayBuffer[Stmt[V]]()

    /**
     * Central dispatcher for emitting or loading a statement’s value during backward code generation.
     *
     * @param defIdx           The TAC statement index defining the variable to process.
     * @param delayStmtVisit   If true, label emission for this statement is delayed until after all its child expressions have been processed.
     * @param nestedStmt       If true, store/load instructions inside this statement are suppressed because it is nested within another.
     * @param parentIdx        The index of the parent statement, used to detect end nodes.
     */
    def emitStmt(defIdx: Int,
                 delayStmtVisit: Boolean = false,
                 nestedStmt: Boolean = false,
                 parentIdx: Int = -1): Unit = {
        val variable = getVarFromId(defIdx)
        val stmt = tacStmts(defIdx)._1
        val stmtIndex = tacStmts(defIdx)._2
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)
        val childIdx = findChildIndex(variable)

        // Mark this statement as an end node when it represents the final use and the variable is already in locals
        if (childIdx == parentIdx && loadedStmt.contains(variable)) {
            endNodes += stmt
        }

        // If this def has multiple defs or uses, load from locals; otherwise, process it normally
        if (getDefSize(childIdx, defIdx) > 1 || getUseSites(defIdx) > 1) {
            emitVarDef(variable)
        } else {
            StmtProcessor.processStmt(stmt, tacToLVIndex, labels, code, this, stmtIndex, delayStmtVisit, nestedStmt)
        }
    }

    /**
     * Handles the use of end nodes during backward bytecode generation.
     *
     * @param variable         The variable whose use is being processed.
     * @param delayStmtVisit   If true, label emission for this statement is delayed until after all its child expressions have been processed.
     * @param nestedStmt       If true, store/load instructions inside this statement are suppressed because it is nested within another.
     */
    def emitVarUse(variable: Var[V],
                   delayStmtVisit: Boolean = false,
                   nestedStmt: Boolean): Unit = {
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)
        val defSites = getIndicesFromVariable(variable)
        val defIdx = defSites.head

        val childIdx = findChildIndex(variable)

        // If the current expression has multiple def- and use-sites, store the variable in a local
        if (getDefSize(childIdx, defIdx) > 1 || getUseSites(defIdx) > 1) {
            ExprProcessor.storeVariable(variable, tacToLVIndex, code)
        }

        // Otherwise emit its defining bytecode sequence
        emitDef(defIdx, delayStmtVisit, nestedStmt)
    }

    /**
     * Handles variables with multiple definition sites.
     */
    def emitVarDef(variable: Var[V]): Unit = {
        if (!savedDefSites.contains(variable)) saveVariableInfo(variable)
        ExprProcessor.loadVariable(variable, tacToLVIndex, code)
        loadedStmt += variable
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

    private def findChildIndex(variable: Var[V]): Int = {
        variable match {
            case dvar: DVar[ValueInformation] => dvar.usedBy.iterator.min
            case uvar: UVar[ValueInformation] => uvar.definedBy.toList.iterator.min
        }
    }

    /**
     * Emits bytecode for the definition of a variable at the given index.
     * Loads a constant onto the stack.
     */
    private def emitDef(defIdx: Int, delayStmtVisit: Boolean, nestedStmt: Boolean): Unit = {
        val stmt = tacStmts(defIdx)._1
        val stmtIndex = tacStmts(defIdx)._2
        stmt match {
            case Assignment(_, _, expr) =>
                expr match {
                    case const: Const => ExprProcessor.loadConstant(const, code)
                    case newArray: NewArray[V] => ExprProcessor.processNewArray(newArray, tacToLVIndex, code, this)
                    case newExpr: New => ExprProcessor.processNewExpr(newExpr.tpe, code)
                    case getStatic: GetStatic => ExprProcessor.processGetStatic(getStatic, code)
                    case callExpr: Call[V @unchecked] =>
                        val call @ Call(declaringClass, isInterface, name, descriptor) = callExpr
                        processCall(
                            call,
                            declaringClass,
                            isInterface,
                            name,
                            descriptor,
                            tacToLVIndex,
                            code,
                            this,
                            stmtIndex
                        )
                    case arrayLoadExpr: ArrayLoad[V] => processArrayLoad(arrayLoadExpr, tacToLVIndex, code, this, delayStmtVisit, stmtIndex)
                    case binaryExpr: BinaryExpr[V] => processBinaryExpr(binaryExpr, tacToLVIndex, code, this, nestedStmt, stmtIndex)
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
            case Switch(_, _, index, _) =>
                findUVarInExpr(index, defIdx)
                    .map(_.asVar.definedBy.size)
                    .getOrElse(0)
            case _ => 0
        }
    }

    /**
     * Returns the variable corresponding to the given definition index.
     */
    def getVarFromId(defIdx: Int): Var[V] = {
        tacStmts(defIdx)._1 match {
            case Assignment(_, dvar: DVar[ValueInformation], _) => dvar.asVar
            case ExprStmt(_, _) => null
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

    def isStmtLoaded(stmtIndex: Int): Boolean = {
        loadedStmt.exists {
            case dvar: DVar[ValueInformation] => dvar.originatedAt == stmtIndex
            case uvar: UVar[ValueInformation] => uvar.definedBy.head == stmtIndex
            case _ => false
        }
    }

    def isStmtMarkedAsEndNode(stmtIdx: Int): Boolean = {
        val stmt = tacStmts(stmtIdx)._1
        endNodes.contains(stmt)
    }
}
