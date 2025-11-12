/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldassignability

import scala.annotation.switch

import scala.collection.mutable
import scala.util.boundary
import scala.util.boundary.break

import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.NE
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.ClassType
import org.opalj.br.DoubleType
import org.opalj.br.FieldType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.immutability.Assignable
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.br.fpcf.properties.immutability.LazilyInitialized
import org.opalj.br.fpcf.properties.immutability.UnsafelyLazilyInitialized
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.CaughtException
import org.opalj.tac.ClassConst
import org.opalj.tac.Compare
import org.opalj.tac.Expr
import org.opalj.tac.FieldWriteAccessStmt
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.If
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.Throw
import org.opalj.tac.VirtualFunctionCall

trait LazyInitializationAnalysisState extends AbstractFieldAssignabilityAnalysisState {
    var potentialLazyInit: Option[(Context, Int, Int, TACode[TACMethodParameter, V])] = None
}

/**
 * Determines whether a field write access corresponds to a lazy initialization of the field.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 * @author Maximilian Rüsch
 */
trait LazyInitializationAnalysis private[fieldassignability]
    extends FieldAssignabilityAnalysisPart {

    override type AnalysisState <: LazyInitializationAnalysisState

    val considerLazyInitialization: Boolean =
        project.config.getBoolean(
            "org.opalj.fpcf.analyses.L2FieldAssignabilityAnalysis.considerLazyInitialization"
        )

    /**
     * Returns the initialization value of a given type.
     */
    private def fieldDefaultValues(implicit state: AnalysisState): Set[Any] = state.field.fieldType match {
        case FloatType | ClassType.Float                                                               => Set(0.0f)
        case DoubleType | ClassType.Double                                                             => Set(0.0d)
        case LongType | ClassType.Long                                                                 => Set(0L)
        case CharType | ClassType.Character                                                            => Set('\u0000')
        case BooleanType | ClassType.Boolean                                                           => Set(false)
        case IntegerType | ClassType.Integer | ByteType | ClassType.Byte | ShortType | ClassType.Short => Set(0)
        case ClassType.String                                                                          => Set("", null)
        case _: ReferenceType                                                                          => Set(null)
    }

    /**
     * Determines whether the basic block of a given index dominates the basic block of the other index or is executed
     * before the other index in the case of both indexes belonging to the same basic block.
     */
    private def dominates(
        potentiallyDominatorIndex: Int,
        potentiallyDominatedIndex: Int,
        taCode:                    TACode[TACMethodParameter, V]
    ): Boolean = {
        val bbPotentiallyDominator = taCode.cfg.bb(potentiallyDominatorIndex)
        val bbPotentiallyDominated = taCode.cfg.bb(potentiallyDominatedIndex)
        taCode.cfg.dominatorTree
            .strictlyDominates(bbPotentiallyDominator.nodeId, bbPotentiallyDominated.nodeId) ||
            bbPotentiallyDominator == bbPotentiallyDominated && potentiallyDominatorIndex < potentiallyDominatedIndex
    }

    override def completePatternWithInitializerRead()(implicit state: AnalysisState): Option[FieldAssignability] =
        state.potentialLazyInit.map(_ => Assignable)

    override def completePatternWithNonInitializerRead(
        context: Context,
        readPC:  Int
    )(implicit state: AnalysisState): Option[FieldAssignability] = {
        // No lazy init pattern exists, or it was not discovered yet
        if (state.potentialLazyInit.isEmpty)
            return None;

        val (lazyInitContext, guardIndex, writeIndex, tac) = state.potentialLazyInit.get
        // We only support lazy initialization patterns fully contained within one method, but different contexts of the
        // same method are fine.
        if (context.method ne lazyInitContext.method)
            return Some(Assignable);
        if (context.id != lazyInitContext.id)
            return None;

        if (doFieldReadsEscape(Set(readPC), guardIndex, writeIndex, tac))
            return Some(Assignable);

        None
    }

    override def completePatternWithInitializerWrite()(implicit state: AnalysisState): Option[FieldAssignability] =
        state.potentialLazyInit.map(_ => Assignable)

    override def completePatternWithNonInitializerWrite(
        context:  Context,
        tac:      TACode[TACMethodParameter, V],
        writePC:  PC,
        receiver: Option[V]
    )(implicit state: AnalysisState): Option[FieldAssignability] = {
        if (state.field.isNotStatic) {
            if (receiver.isEmpty)
                return Some(Assignable);

            if (receiver.get.definedBy != SelfReferenceParameter)
                return None;
        }

        // A lazy initialization pattern does not allow initializing a field regularly
        if (state.initializerWrites.nonEmpty)
            return Some(Assignable);

        // Multiple lazy initialization patterns cannot be supported in a collaborative setting
        if (state.nonInitializerWrites.iterator.distinctBy(_._1.method).size > 1)
            return Some(Assignable);

        // We do not support multiple-write lazy initializations yet
        if (state.nonInitializerWrites(context).size > 1)
            return Some(Assignable);

        // A lazy init does not allow reads outside the lazy initialization method, effectively also preventing analysis
        // of patterns with multiple lazy-init functions.
        if (state.initializerReads.nonEmpty || state.nonInitializerReads.exists(_._1.method ne context.method))
            return Some(Assignable);

        val method = context.method.definedMethod
        val writeIndex = tac.pcToIndex(writePC)
        val cfg = tac.cfg
        val writeBB = cfg.bb(writeIndex).asBasicBlock

        // We only support lazy initialization using direct field writes
        if (!tac.stmts(writeIndex).isFieldWriteAccessStmt)
            return Some(Assignable);
        val writeStmt = tac.stmts(writeIndex).asFieldWriteAccessStmt

        val resultCatchesAndThrows = findCatchesAndThrows(tac)
        val findGuardsResult = findGuards(writeIndex, tac)
        // no guard -> no lazy initialization
        if (findGuardsResult.isEmpty)
            return Some(Assignable);

        val (readIndex, guardIndex, defaultCaseIndex, elseCaseIndex) = findGuardsResult.head

        // The field has to be written when the guard is in the default-case branch
        if (!dominates(defaultCaseIndex, writeIndex, tac))
            return Some(Assignable);

        val elseBB = cfg.bb(elseCaseIndex)

        // prevents wrong control flow
        if (isTransitivePredecessor(elseBB, writeBB))
            return Some(Assignable);

        if (method.returnType == state.field.fieldType) {
            // prevents that another value than the field value is returned with the same type
            if (!isFieldValueReturned(writeStmt, writeIndex, readIndex, tac, findGuardsResult))
                return Some(Assignable);
            // prevents that the field is seen with another value
            if ( // potentially unsound with method.returnType == state.field.fieldType
                 // TODO comment it out and look at appearing cases
                 tac.stmts.exists(stmt =>
                     stmt.isReturnValue && !isTransitivePredecessor(
                         writeBB,
                         cfg.bb(tac.pcToIndex(stmt.pc))
                     ) &&
                         findGuardsResult.forall { // TODO check...
                             case (indexOfFieldRead, _, _, _) =>
                                 !isTransitivePredecessor(
                                     cfg.bb(indexOfFieldRead),
                                     cfg.bb(tac.pcToIndex(stmt.pc))
                                 )
                         }
                 )
            )
                return Some(Assignable);
        }

        if (doFieldReadsEscape(state.nonInitializerReads(context).map(_._1), guardIndex, writeIndex, tac))
            return Some(Assignable);

        state.potentialLazyInit = Some(context, guardIndex, writeIndex, tac)

        /**
         * Determines whether all caught exceptions are thrown afterwards
         */
        def noInterferingExceptions(): Boolean =
            resultCatchesAndThrows._1.forall {
                case (catchPC, originsCaughtException) =>
                    resultCatchesAndThrows._2.exists {
                        case (throwPC, throwDefinitionSites) =>
                            dominates(tac.pcToIndex(catchPC), tac.pcToIndex(throwPC), tac) &&
                                throwDefinitionSites == originsCaughtException // throwing and catching same exceptions
                    }
            }

        if (writeStmt.value.asVar.definedBy.forall(_ >= 0) &&
            dominates(defaultCaseIndex, writeIndex, tac) &&
            noInterferingExceptions()
        ) {
            if (method.isSynchronized)
                Some(LazilyInitialized)
            else {
                val (indexMonitorEnter, indexMonitorExit) = findMonitors(writeIndex, tac)
                val monitorResultsDefined = indexMonitorEnter.isDefined && indexMonitorExit.isDefined
                if (monitorResultsDefined && dominates(indexMonitorEnter.get, readIndex, tac))
                    Some(LazilyInitialized)
                else
                    Some(UnsafelyLazilyInitialized)
            }
        } else
            Some(Assignable)
    }

    private def doFieldReadsEscape(
        reads:      Set[PC],
        guardIndex: Int,
        writeIndex: Int,
        tac:        TACode[TACMethodParameter, V]
    )(implicit state: AnalysisState): Boolean = boundary {
        var seen: Set[Stmt[V]] = Set.empty

        def doUsesEscape(
            pcs: PCs
        )(implicit state: AnalysisState): Boolean = {
            val cfg = tac.cfg

            pcs.exists(pc => {
                val index = tac.pcToIndex(pc)
                if (index == -1)
                    break(true);
                val stmt = tac.stmts(index)

                if (stmt.isAssignment) {
                    stmt.asAssignment.targetVar.usedBy.exists(i =>
                        i == -1 || {
                            val st = tac.stmts(i)
                            if (!seen.contains(st)) {
                                seen += st
                                !(
                                    st.isReturnValue || st.isIf ||
                                        dominates(guardIndex, i, tac) &&
                                        isTransitivePredecessor(cfg.bb(writeIndex), cfg.bb(i)) ||
                                        (st match {
                                            case AssignmentLikeStmt(_, expr) =>
                                                (expr.isCompare || expr.isFunctionCall && {
                                                    val functionCall = expr.asFunctionCall
                                                    state.field.fieldType match {
                                                        case ClassType.Byte    => functionCall.name == "byteValue"
                                                        case ClassType.Short   => functionCall.name == "shortValue"
                                                        case ClassType.Integer => functionCall.name == "intValue"
                                                        case ClassType.Long    => functionCall.name == "longValue"
                                                        case ClassType.Float   => functionCall.name == "floatValue"
                                                        case ClassType.Double  => functionCall.name == "doubleValue"
                                                        case _                 => false
                                                    }
                                                }) && !doUsesEscape(st.asAssignment.targetVar.usedBy)
                                            case _ => false
                                        })
                                )
                            } else false
                        }
                    )
                } else false
            })
        }

        reads.exists { pc => doUsesEscape(IntTrieSet(pc)) }
    }

    /**
     * This method returns the information about catch blocks, throw statements and return nodes
     *
     * @note It requires still determined taCode
     *
     * @return The first element of the tuple returns:
     *         the caught exceptions (the pc of the catch, the exception type, the origin of the caught exception,
     *         the bb of the caughtException)
     * @return The second element of the tuple returns:
     *         The throw statements: (the pc, the definitionSites, the bb of the throw statement)
     * @author Tobias Roth
     */
    private def findCatchesAndThrows(
        tacCode: TACode[TACMethodParameter, V]
    ): (List[(Int, IntTrieSet)], List[(Int, IntTrieSet)]) = {
        var caughtExceptions: List[(Int, IntTrieSet)] = List.empty
        var throwStatements: List[(Int, IntTrieSet)] = List.empty
        for (stmt <- tacCode.stmts) {
            if (!stmt.isNop) { // to prevent the handling of partially negative pcs of nops
                (stmt.astID: @switch) match {

                    case CaughtException.ASTID =>
                        val caughtException = stmt.asCaughtException
                        caughtExceptions = (caughtException.pc, caughtException.origins) :: caughtExceptions

                    case Throw.ASTID =>
                        val throwStatement = stmt.asThrow
                        val throwStatementDefinedBys = throwStatement.exception.asVar.definedBy
                        throwStatements = (throwStatement.pc, throwStatementDefinedBys) :: throwStatements

                    case _ =>
                }
            }
        }
        (caughtExceptions, throwStatements)
    }

    /**
     * Searches the closest monitor enter and exit to the field write.
     * @return the index of the monitor enter and exit
     * @author Tobias Roth
     */
    private def findMonitors(
        writeIndex: Int,
        tac:        TACode[TACMethodParameter, V]
    )(implicit state: LazyInitializationAnalysisState): (Option[Int], Option[Int]) = {

        var result: (Option[Int], Option[Int]) = (None, None)
        val startBB = tac.cfg.bb(writeIndex)
        var monitorExitQueuedBBs: Set[CFGNode] = startBB.successors
        var worklistMonitorExit = getSuccessors(startBB, Set.empty)

        /**
         * checks that a given monitor supports a thread safe lazy initialization.
         * Supports two ways of synchronized blocks.
         *
         * When determining the lazy initialization of a static field,
         * it allows only global locks on Foo.class. Independent of which class Foo is.
         *
         * When determining the lazy initialization of an instance fields, it allows
         * synchronized(this) and synchronized(Foo.class). Independent of which class Foo is.
         * In case of an instance field the second case is even stronger than synchronized(this).
         */
        def checkMonitor(v: V)(implicit state: LazyInitializationAnalysisState): Boolean = {
            v.definedBy.forall(definedByIndex => {
                if (definedByIndex >= 0) {
                    tac.stmts(definedByIndex) match {
                        // synchronized(Foo.class)
                        case Assignment(_, _, _: ClassConst) => true
                        case _                               => false
                    }
                } else {
                    // synchronized(this)
                    state.field.isNotStatic && IntTrieSet(definedByIndex) == SelfReferenceParameter
                }
            })
        }

        var monitorEnterQueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklistMonitorEnter = getPredecessors(startBB, Set.empty)

        // find monitorenter
        while (worklistMonitorEnter.nonEmpty) {
            val curBB = worklistMonitorEnter.head
            worklistMonitorEnter = worklistMonitorEnter.tail
            val startPC = curBB.startPC
            val endPC = curBB.endPC
            var hasNotFoundAnyMonitorYet = true
            for (i <- startPC to endPC) {
                (tac.stmts(i).astID: @switch) match {
                    case MonitorEnter.ASTID =>
                        val monitorEnter = tac.stmts(i).asMonitorEnter
                        if (checkMonitor(monitorEnter.objRef.asVar)) {
                            result = (Some(tac.pcToIndex(monitorEnter.pc)), result._2)
                            hasNotFoundAnyMonitorYet = false
                        }
                    case _ =>
                }
            }
            if (hasNotFoundAnyMonitorYet) {
                val predecessor = getPredecessors(curBB, monitorEnterQueuedBBs)
                worklistMonitorEnter ++= predecessor
                monitorEnterQueuedBBs ++= predecessor
            }
        }
        // find monitorexit
        while (worklistMonitorExit.nonEmpty) {
            val curBB = worklistMonitorExit.head

            worklistMonitorExit = worklistMonitorExit.tail
            val endPC = curBB.endPC

            val cfStmt = tac.stmts(endPC)
            (cfStmt.astID: @switch) match {

                case MonitorExit.ASTID =>
                    val monitorExit = cfStmt.asMonitorExit
                    if (checkMonitor(monitorExit.objRef.asVar)) {
                        result = (result._1, Some(tac.pcToIndex(monitorExit.pc)))
                    }

                case _ =>
                    val successors = getSuccessors(curBB, monitorExitQueuedBBs)
                    worklistMonitorExit ++= successors
                    monitorExitQueuedBBs ++= successors
            }
        }
        result
    }

    /**
     * Finds the indexes of the guarding if-Statements for a lazy initialization, the index of the
     * first statement executed if the field does not have its default value and the index of the
     * field read used for the guard and the index of the field-read.
     */
    private def findGuards(
        fieldWrite: Int,
        taCode:     TACode[TACMethodParameter, V]
    )(implicit state: AnalysisState): List[(Int, Int, Int, Int)] = {
        val cfg = taCode.cfg
        val code = taCode.stmts

        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklist: List[BasicBlock] = getPredecessors(startBB, Set.empty)
        var seen: Set[BasicBlock] = Set.empty
        var result: List[(Int, Int, Int)] = List.empty /* guard pc, true target pc, false target pc */

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail
            if (!seen.contains(curBB)) {
                seen += curBB

                val endPC = curBB.endPC

                val cfStmt = code(endPC)
                (cfStmt.astID: @switch) match {

                    case If.ASTID =>
                        val ifStmt = cfStmt.asIf
                        if (ifStmt.condition.equals(EQ) && curBB != startBB && isGuard(
                                ifStmt,
                                fieldDefaultValues,
                                code,
                                taCode
                            )
                        ) {
                            result = (endPC, ifStmt.targetStmt, endPC + 1) :: result
                        } else if (ifStmt.condition.equals(NE) && curBB != startBB && isGuard(
                                       ifStmt,
                                       fieldDefaultValues,
                                       code,
                                       taCode
                                   )
                        ) {
                            result = (endPC, endPC + 1, ifStmt.targetStmt) :: result
                        } else {
                            if ((cfg.bb(fieldWrite) != cfg.bb(ifStmt.target) || fieldWrite < ifStmt.target) &&
                                isTransitivePredecessor(cfg.bb(fieldWrite), cfg.bb(ifStmt.target))
                            ) {
                                return List.empty // in cases where other if-statements destroy
                            }
                        }
                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors

                    // Otherwise, we have to ensure that a guard is present for all predecessors
                    case _ =>
                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors
                }
            }

        }

        var finalResult: List[(Int, Int, Int, Int)] = List.empty
        var fieldReadIndex = 0
        result.foreach { case (guardPC, trueTargetPC, falseTargetPC) =>
            // The field read that defines the value checked by the guard must be used only for the
            // guard or directly if the field's value was not the default value
            val ifStmt = code(guardPC).asIf

            val expr =
                if (ifStmt.leftExpr.isConst) ifStmt.rightExpr
                else ifStmt.leftExpr

            val definitions = expr.asVar.definedBy
            if (definitions.forall(_ >= 0)) {

                fieldReadIndex = definitions.head

                val fieldReadUses = code(definitions.head).asAssignment.targetVar.usedBy

                val fieldReadUsedCorrectly =
                    fieldReadUses.forall(use => use == guardPC || use == falseTargetPC)

                if (definitions.size == 1 && definitions.head >= 0 && fieldReadUsedCorrectly) {
                    // Found proper guard
                    finalResult = (fieldReadIndex, guardPC, trueTargetPC, falseTargetPC) :: finalResult
                }
            }
        }
        finalResult
    }

    /**
     * Returns all predecessor BasicBlocks of a CFGNode.
     */
    private def getPredecessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        def getPredecessorsInternal(node: CFGNode, visited: Set[CFGNode]): Iterator[BasicBlock] = {
            node.predecessors.iterator.flatMap { currentNode =>
                if (currentNode.isBasicBlock) {
                    if (visited.contains(currentNode))
                        None
                    else
                        Some(currentNode.asBasicBlock)
                } else
                    getPredecessorsInternal(currentNode, visited)
            }
        }
        getPredecessorsInternal(node, visited).toList
    }

    /**
     * Determines whether a node is a transitive predecessor of another node.
     */
    private def isTransitivePredecessor(possiblePredecessor: CFGNode, node: CFGNode): Boolean = {
        val visited: mutable.Set[CFGNode] = mutable.Set.empty
        def isTransitivePredecessorInternal(possiblePredecessor: CFGNode, node: CFGNode): Boolean = {
            if (possiblePredecessor == node)
                true
            else if (visited.contains(node))
                false
            else {
                visited += node
                node.predecessors.exists(currentNode => isTransitivePredecessorInternal(possiblePredecessor, currentNode))
            }
        }
        isTransitivePredecessorInternal(possiblePredecessor, node)
    }

    /**
     * Returns all successors BasicBlocks of a CFGNode
     */
    private def getSuccessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        def getSuccessorsInternal(node: CFGNode, visited: Set[CFGNode]): Iterator[BasicBlock] = {
            node.successors.iterator flatMap { currentNode =>
                if (currentNode.isBasicBlock)
                    if (visited.contains(currentNode)) None
                    else Some(currentNode.asBasicBlock)
                else getSuccessors(currentNode, visited)
            }
        }
        getSuccessorsInternal(node, visited).toList
    }

    /**
     * Checks if an expression is a field read of the currently analyzed field.
     * For instance fields, the read must be on the `this` reference.
     */
    private def isReadOfCurrentField(
        expr:    Expr[V],
        tacCode: TACode[TACMethodParameter, V],
        index:   Int
    )(implicit state: LazyInitializationAnalysisState): Boolean = {
        def isExprReadOfCurrentField: Int => Boolean =
            exprIndex =>
                exprIndex == index ||
                    exprIndex >= 0 && isReadOfCurrentField(
                        tacCode.stmts(exprIndex).asAssignment.expr,
                        tacCode,
                        exprIndex
                    )
        (expr.astID: @switch) match {
            case GetField.ASTID =>
                val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                if (objRefDefinition != SelfReferenceParameter) false
                else expr.asGetField.resolveField(using project).contains(state.field)

            case GetStatic.ASTID             => expr.asGetStatic.resolveField(using project).contains(state.field)
            case PrimitiveTypecastExpr.ASTID => false

            case Compare.ASTID =>
                val leftExpr = expr.asCompare.left
                val rightExpr = expr.asCompare.right
                leftExpr.asVar.definedBy
                    .forall(index => index >= 0 && tacCode.stmts(index).asAssignment.expr.isConst) &&
                    rightExpr.asVar.definedBy.forall(isExprReadOfCurrentField) ||
                    rightExpr.asVar.definedBy
                        .forall(index => index >= 0 && tacCode.stmts(index).asAssignment.expr.isConst) &&
                    leftExpr.asVar.definedBy.forall(isExprReadOfCurrentField)

            case VirtualFunctionCall.ASTID =>
                val functionCall = expr.asVirtualFunctionCall
                val fieldType = state.field.fieldType
                functionCall.params.isEmpty && (
                    fieldType match {
                        case ClassType.Byte    => functionCall.name == "byteValue"
                        case ClassType.Short   => functionCall.name == "shortValue"
                        case ClassType.Integer => functionCall.name == "intValue"
                        case ClassType.Long    => functionCall.name == "longValue"
                        case ClassType.Float   => functionCall.name == "floatValue"
                        case ClassType.Double  => functionCall.name == "doubleValue"
                        case _                 => false
                    }
                ) && functionCall.receiver.asVar.definedBy.forall(isExprReadOfCurrentField)

            case _ => false
        }
    }

    /**
     * Determines if an if-Statement is actually a guard for the current field, i.e. it compares
     * the current field to the default value.
     */
    private def isGuard(
        ifStmt:        If[V],
        defaultValues: Set[Any],
        code:          Array[Stmt[V]],
        tacCode:       TACode[TACMethodParameter, V]
    )(implicit state: AnalysisState): Boolean = {

        def isDefaultConst(expr: Expr[V]): Boolean = {

            if (expr.isVar) {
                val defSites = expr.asVar.definedBy
                defSites.size == 1 && defSites.forall(_ >= 0) &&
                    defSites.forall(defSite => isDefaultConst(code(defSite).asAssignment.expr))
            } else {
                // default value check
                expr.isIntConst && defaultValues.contains(expr.asIntConst.value) ||
                expr.isFloatConst && defaultValues.contains(expr.asFloatConst.value) ||
                expr.isDoubleConst && defaultValues.contains(expr.asDoubleConst.value) ||
                expr.isLongConst && defaultValues.contains(expr.asLongConst.value) ||
                expr.isStringConst && defaultValues.contains(expr.asStringConst.value) ||
                expr.isNullExpr && defaultValues.contains(null)
            }
        }

        /**
         * Checks whether the non-constant expression of the if-Statement is a read of the current
         * field.
         */
        def isGuardInternal(
            expr:    V,
            tacCode: TACode[TACMethodParameter, V]
        ): Boolean = {
            expr.definedBy forall { index =>
                if (index < 0)
                    false // If the value is from a parameter, this can not be the guard
                else {
                    val expression = code(index).asAssignment.expr
                    // in case of Integer etc.... .initValue()
                    if (expression.isVirtualFunctionCall) {
                        val virtualFunctionCall = expression.asVirtualFunctionCall
                        virtualFunctionCall.receiver.asVar.definedBy.forall(receiverDefSite =>
                            receiverDefSite >= 0 &&
                                isReadOfCurrentField(code(receiverDefSite).asAssignment.expr, tacCode, index)
                        )
                    } else
                        isReadOfCurrentField(expression, tacCode, index)
                }
            }
        }

        // Special handling for these types needed because of compare function in bytecode
        def hasFloatDoubleOrLongType(fieldType: FieldType): Boolean =
            fieldType.isFloatType || fieldType.isDoubleType || fieldType.isLongType

        if (ifStmt.rightExpr.isVar && hasFloatDoubleOrLongType(state.field.fieldType) &&
            ifStmt.rightExpr.asVar.definedBy.head > 0 &&
            tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.isCompare
        ) {

            val left =
                tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            val right =
                tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr

            if (leftExpr.isGetField || leftExpr.isGetStatic) isDefaultConst(rightExpr)
            else (rightExpr.isGetField || rightExpr.isGetStatic) && isDefaultConst(leftExpr)

        } else if (ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar && ifStmt.leftExpr.asVar.definedBy.head >= 0 &&
                   ifStmt.rightExpr.asVar.definedBy.head >= 0 &&
                   hasFloatDoubleOrLongType(state.field.fieldType) && tacCode
                       .stmts(ifStmt.leftExpr.asVar.definedBy.head)
                       .asAssignment
                       .expr
                       .isCompare &&
                   ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar
        ) {

            val left =
                tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            val right =
                tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr

            if (leftExpr.isGetField || leftExpr.isGetStatic) isDefaultConst(rightExpr)
            else (rightExpr.isGetField || rightExpr.isGetStatic) && isDefaultConst(leftExpr)

        } else if (ifStmt.rightExpr.isVar && isDefaultConst(ifStmt.leftExpr)) {
            isGuardInternal(ifStmt.rightExpr.asVar, tacCode)
        } else if (ifStmt.leftExpr.isVar && isDefaultConst(ifStmt.rightExpr)) {
            isGuardInternal(ifStmt.leftExpr.asVar, tacCode)
        } else false
    }

    /**
     * Checks that the returned value is definitely read from the field.
     */
    private def isFieldValueReturned(
        write:        FieldWriteAccessStmt[V],
        writeIndex:   Int,
        readIndex:    Int,
        tac:          TACode[TACMethodParameter, V],
        guardIndexes: List[(Int, Int, Int, Int)]
    )(implicit state: AnalysisState): Boolean = {

        def isSimpleReadOfField(expr: Expr[V]) =
            expr.astID match {

                case GetField.ASTID =>
                    val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                    if (objRefDefinition != SelfReferenceParameter)
                        false
                    else
                        expr.asGetField.resolveField(using project).contains(state.field)

                case GetStatic.ASTID => expr.asGetStatic.resolveField(using project).contains(state.field)

                case _ => false
            }

        tac.stmts.forall { stmt =>
            !stmt.isReturnValue || {

                val returnValueDefs = stmt.asReturnValue.expr.asVar.definedBy
                val assignedValueDefSite = write.value.asVar.definedBy
                returnValueDefs.forall(_ >= 0) && {
                    if (returnValueDefs.size == 1 && returnValueDefs.head != readIndex) {
                        val expr = tac.stmts(returnValueDefs.head).asAssignment.expr
                        isSimpleReadOfField(expr) && guardIndexes.exists {
                            case (_, guardIndex, defaultCase, _) =>
                                dominates(guardIndex, returnValueDefs.head, tac) &&
                                    (!dominates(defaultCase, returnValueDefs.head, tac) ||
                                    dominates(writeIndex, returnValueDefs.head, tac))
                        }
                    } // The field is either read before the guard and returned or
                    // the value assigned to the field is returned
                    else {
                        returnValueDefs.size == 2 && assignedValueDefSite.size == 1 &&
                        returnValueDefs.contains(readIndex) && {
                            returnValueDefs.contains(assignedValueDefSite.head) || {
                                val potentiallyReadIndex = returnValueDefs.filter(_ != readIndex).head
                                val expr = tac.stmts(potentiallyReadIndex).asAssignment.expr
                                isSimpleReadOfField(expr) &&
                                    guardIndexes.exists {
                                        case (_, guardIndex, defaultCase, _) =>
                                            dominates(guardIndex, potentiallyReadIndex, tac) &&
                                                (!dominates(defaultCase, returnValueDefs.head, tac) ||
                                                dominates(writeIndex, returnValueDefs.head, tac))
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}
