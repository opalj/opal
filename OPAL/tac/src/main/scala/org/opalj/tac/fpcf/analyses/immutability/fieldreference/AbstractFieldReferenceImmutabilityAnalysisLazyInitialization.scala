/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldreference

import scala.annotation.tailrec
import scala.collection.mutable

import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.NE
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
import org.opalj.br.fpcf.properties.MutableFieldReference
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.ObjectType
import scala.annotation.switch
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.FieldType

/**
 *
 * Encompasses the base functions for determining lazy initialization of a field reference.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 *
 */
trait AbstractFieldReferenceImmutabilityAnalysisLazyInitialization extends AbstractFieldReferenceImmutabilityAnalysis
    with FPCFAnalysis {

    /**
     * handles the lazy initialization determination for a field write in a given method
     * @author Tobias Roth
     * @return true if there is no thread safe or deterministic lazy initialization
     */
    def handleLazyInitialization(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        taCode:       TACode[TACMethodParameter, V]
    )(implicit state: State): Boolean = {
        val lazyInitializationResult: FieldReferenceImmutability =
            determineLazyInitialization(writeIndex, defaultValue, method, taCode)

        state.referenceImmutability = lazyInitializationResult
        lazyInitializationResult == MutableFieldReference
    }

    /**
     * Determines whether the basic block of a given index dominates the basic block of the other or is executed
     * before the other when the basic blocks are the same
     */
    def dominates(
        potentiallyDominator: Int,
        potentiallyDominated: Int, taCode: TACode[TACMethodParameter, V]
    ): Boolean = {
        val bbPotentiallyDominator = taCode.cfg.bb(potentiallyDominator)
        val bbPotentiallyDominated = taCode.cfg.bb(potentiallyDominated)

        taCode.cfg.dominatorTree.strictlyDominates(bbPotentiallyDominator.nodeId, bbPotentiallyDominated.nodeId) ||
            bbPotentiallyDominator == bbPotentiallyDominated && potentiallyDominator < potentiallyDominated
    }

    /**
     * Determines whether a given field is lazy initialized in the given method through a given field write.
     * @author Tobias Roth
     */
    def determineLazyInitialization(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        taCode:       TACode[TACMethodParameter, V]
    )(implicit state: State): FieldReferenceImmutability = {
        val code = taCode.stmts
        val cfg = taCode.cfg

        val write = code(writeIndex).asFieldWriteAccessStmt
        val writeBB = cfg.bb(writeIndex).asBasicBlock
        val resultCatchesAndThrows = findCatchesAndThrows(taCode)

        /**
         * Determines whether all exceptions that are caught are thrown
         */
        def noInterferingExceptions(): Boolean = {
            resultCatchesAndThrows._1.forall {
                case (catchPC, originsCaughtException) ⇒
                    resultCatchesAndThrows._2.exists {
                        case (throwPC, throwDefinitionSites) ⇒
                            dominates(taCode.pcToIndex(catchPC), taCode.pcToIndex(throwPC), taCode) &&
                                throwDefinitionSites == originsCaughtException //throwing and catching same exceptions
                    }
            }
        }

        val findGuardResult = findGuards(method, writeIndex, defaultValue, taCode)

        val (readIndex, guardIndex, defaultCaseIndex, elseCaseIndex) = //guardIndex: for debugging purposes
            if (findGuardResult.nonEmpty)
                (findGuardResult.head._1, findGuardResult.head._2, findGuardResult.head._3, findGuardResult.head._4)
            else
                return MutableFieldReference;

        if (!dominates(defaultCaseIndex, writeIndex, taCode)) return MutableFieldReference;

        val elseBB = cfg.bb(elseCaseIndex)

        // prevents wrong control flow
        if (isTransitivePredecessor(elseBB, writeBB))
            return MutableFieldReference;

        if (method.returnType == state.field.fieldType) {
            // prevents that another value than the field value is returned with the same type
            if (!isFieldValueReturned(write, writeIndex, readIndex, taCode, findGuardResult))
                return MutableFieldReference;
            //prevents that the field is seen with another value
            if ( // potentially unsound with method.returnType == state.field.fieldType
            // TODO comment it out and look at appearing cases
            taCode.stmts.exists(stmt ⇒ stmt.isReturnValue &&
                !isTransitivePredecessor(writeBB, cfg.bb(taCode.pcToIndex(stmt.pc))) &&
                findGuardResult.forall {
                    case (indexOfFieldRead, _, _, _) ⇒
                        !isTransitivePredecessor(cfg.bb(indexOfFieldRead), cfg.bb(taCode.pcToIndex(stmt.pc)))
                }))
                return MutableFieldReference;
        }

        val reads = fieldAccessInformation.readAccesses(state.field)

        // prevents reads outside the method
        if (reads.exists(_._1 ne method))
            return MutableFieldReference;

        val writes = fieldAccessInformation.writeAccesses(state.field)

        // prevents writes outside the method
        // and guarantees that the field is only once written within the method or the constructor
        if (writes.exists(methodAndPCs ⇒ methodAndPCs._2.size > 1 ||
            ((methodAndPCs._1 ne method) && !methodAndPCs._1.isInitializer)))
            return MutableFieldReference;

        // if the method is synchronized the monitor within the method doesn't have to be searched
        if (method.isSynchronized) {
            if (dominates(defaultCaseIndex, writeIndex, taCode) && noInterferingExceptions())
                LazyInitializedThreadSafeFieldReference
            else
                MutableFieldReference
        } else {

            val (indexMonitorEnter, indexMonitorExit) = findMonitors(writeIndex, taCode, method)

            val monitorResultsDefined = indexMonitorEnter.isDefined && indexMonitorExit.isDefined

            if (monitorResultsDefined && //dominates(indexMonitorEnter.get, trueCaseIndex) &&
                dominates(indexMonitorEnter.get, readIndex, taCode)) {
                if (noInterferingExceptions())
                    LazyInitializedThreadSafeFieldReference
                else
                    MutableFieldReference
            } else {
                if (write.value.asVar.definedBy.forall { defSite ⇒
                    defSite >= 0 && checkWriteIsDeterministic(code(defSite).asAssignment, method, code, taCode)
                }
                    && noInterferingExceptions()) {
                    val computationalFieldType = state.field.fieldType.computationalType
                    if (computationalFieldType != ComputationalTypeInt &&
                        computationalFieldType != ComputationalTypeFloat) {
                        LazyInitializedNotThreadSafeFieldReference
                    } else
                        LazyInitializedNotThreadSafeButDeterministicFieldReference
                } else MutableFieldReference
            }
        }
    }

    /**
     *
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
    def findCatchesAndThrows(
        tacCode: TACode[TACMethodParameter, V]
    ): (List[(Int, IntTrieSet)], List[(Int, IntTrieSet)]) = {
        var caughtExceptions: List[(Int, IntTrieSet)] = List.empty
        var throwStatements: List[(Int, IntTrieSet)] = List.empty
        for (stmt ← tacCode.stmts) {
            if (!stmt.isNop) { // to prevent the handling of partially negative pcs of nops
                (stmt.astID: @switch) match {

                    case CaughtException.ASTID ⇒
                        val caughtException = stmt.asCaughtException
                        caughtExceptions = (caughtException.pc, caughtException.origins) :: caughtExceptions

                    case Throw.ASTID ⇒
                        val throwStatement = stmt.asThrow
                        val throwStatementDefinedBys = throwStatement.exception.asVar.definedBy
                        throwStatements = (throwStatement.pc, throwStatementDefinedBys) :: throwStatements

                    case _ ⇒
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
    def findMonitors(
        fieldWrite: Int, tacCode: TACode[TACMethodParameter, V], method: Method
    )(implicit state: State): (Option[Int], Option[Int]) = {

        var result: (Option[Int], Option[Int]) = (None, None)
        val startBB = tacCode.cfg.bb(fieldWrite)
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
         *
         */
        def checkMonitor(v: V)(implicit state: State): Boolean = {
            v.definedBy.forall(definedByIndex ⇒ {
                if (definedByIndex >= 0) {
                    tacCode.stmts(definedByIndex) match {

                        //synchronized(Foo.class)
                        case Assignment(_, _, _: ClassConst) ⇒ true
                        case _                               ⇒ false
                    }
                } else {
                    //synchronized(this)
                    state.field.isNotStatic && IntTrieSet(definedByIndex) == SelfReferenceParameter
                }
            })
        }

        var monitorEnterQueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklistMonitorEnter = getPredecessors(startBB, Set.empty)

        //find monitorenter
        while (worklistMonitorEnter.nonEmpty) {
            val curBB = worklistMonitorEnter.head
            worklistMonitorEnter = worklistMonitorEnter.tail
            val startPC = curBB.startPC
            val endPC = curBB.endPC
            var hasNotFoundAnyMonitorYet = true
            for (i ← startPC to endPC) {
                (tacCode.stmts(i).astID: @switch) match {
                    case MonitorEnter.ASTID ⇒
                        val monitorEnter = tacCode.stmts(i).asMonitorEnter
                        if (checkMonitor(monitorEnter.objRef.asVar)) {
                            result = (Some(tacCode.pcToIndex(monitorEnter.pc)), result._2)
                            hasNotFoundAnyMonitorYet = false
                        }
                    case _ ⇒
                }
            }
            if (hasNotFoundAnyMonitorYet) {
                val predecessor = getPredecessors(curBB, monitorEnterQueuedBBs)
                worklistMonitorEnter ++= predecessor
                monitorEnterQueuedBBs ++= predecessor
            }
        }
        //find monitorexit
        while (worklistMonitorExit.nonEmpty) {
            val curBB = worklistMonitorExit.head

            worklistMonitorExit = worklistMonitorExit.tail
            val endPC = curBB.endPC

            val cfStmt = tacCode.stmts(endPC)
            (cfStmt.astID: @switch) match {

                case MonitorExit.ASTID ⇒
                    val monitorExit = cfStmt.asMonitorExit
                    if (checkMonitor(monitorExit.objRef.asVar)) {
                        result = (result._1, Some(tacCode.pcToIndex(monitorExit.pc)))
                    }

                case _ ⇒
                    val successors = getSuccessors(curBB, monitorExitQueuedBBs)
                    worklistMonitorExit ++= successors
                    monitorExitQueuedBBs ++= successors
            }
        }
        result
    }

    /**
     * Finds the index of the guarding if-Statement for a lazy initialization, the index of the
     * first statement executed if the field does not have its default value and the index of the
     * field read used for the guard and the index of the field-read.
     */
    def findGuards(
        method:       Method,
        fieldWrite:   Int,
        defaultValue: Any,
        taCode:       TACode[TACMethodParameter, V]
    )(implicit state: State): List[(Int, Int, Int, Int)] = {
        val cfg = taCode.cfg
        val code = taCode.stmts

        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklist: List[BasicBlock] = getPredecessors(startBB, Set.empty)
        var seen: Set[BasicBlock] = Set.empty
        var result: List[(Int, Int, Int)] = List.empty

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail
            if (!seen.contains(curBB)) {
                seen += curBB

                val endPC = curBB.endPC

                val cfStmt = code(endPC)
                (cfStmt.astID: @switch) match {

                    case If.ASTID ⇒
                        val ifStmt = cfStmt.asIf
                        if (ifStmt.condition.equals(EQ) && curBB != startBB && isGuard(
                            ifStmt,
                            defaultValue,
                            code,
                            taCode,
                            method
                        )) {
                            result = (endPC, ifStmt.targetStmt, endPC + 1) :: result
                        } else if (ifStmt.condition.equals(NE) && curBB != startBB && isGuard(
                            ifStmt,
                            defaultValue,
                            code,
                            taCode,
                            method
                        )) {
                            result = (endPC, endPC + 1, ifStmt.targetStmt) :: result
                        } else {
                            if ((cfg.bb(fieldWrite) != cfg.bb(ifStmt.target) || fieldWrite < ifStmt.target) &&
                                isTransitivePredecessor(cfg.bb(fieldWrite), cfg.bb(ifStmt.target))) {
                                return List.empty //in cases where other if-statements destroy
                            }
                        }
                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors

                    // Otherwise, we have to ensure that a guard is present for all predecessors
                    case _ ⇒
                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors
                }
            }

        }

        var finalResult: List[(Int, Int, Int, Int)] = List.empty
        var fieldReadIndex = 0
        result.foreach(result ⇒ {
            // The field read that defines the value checked by the guard must be used only for the
            // guard or directly if the field's value was not the default value
            val ifStmt = code(result._1).asIf

            val expr = if (ifStmt.leftExpr.isConst) ifStmt.rightExpr
            else ifStmt.leftExpr

            val definitions = expr.asVar.definedBy
            if (definitions.forall(_ >= 0)) {

                fieldReadIndex = definitions.head

                val fieldReadUses = code(definitions.head).asAssignment.targetVar.usedBy

                val fieldReadUsedCorrectly =
                    fieldReadUses.forall(use ⇒ use == result._1 || use == result._3)

                if (definitions.size == 1 && definitions.head >= 0 && fieldReadUsedCorrectly) {
                    // Found proper guard
                    finalResult = (fieldReadIndex, result._1, result._2, result._3) :: finalResult
                }
            }
        })
        finalResult
    }

    /**
     * Returns all predecessor BasicBlocks of a CFGNode.
     */
    def getPredecessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        def getPredecessorsInternal(node: CFGNode, visited: Set[CFGNode]): Iterator[BasicBlock] = {
            node.predecessors.iterator.flatMap { currentNode ⇒
                if (currentNode.isBasicBlock)
                    if (visited.contains(currentNode)) None
                    else Some(currentNode.asBasicBlock)
                else getPredecessorsInternal(currentNode, visited)
            }
        }
        getPredecessorsInternal(node, visited).toList
    }

    def isTransitivePredecessor(possiblePredecessor: CFGNode, node: CFGNode): Boolean = {

        val visited: mutable.Set[CFGNode] = mutable.Set.empty

        def isTransitivePredecessorInternal(possiblePredecessor: CFGNode, node: CFGNode): Boolean = {
            if (possiblePredecessor == node) true
            else if (visited.contains(node)) false
            else {
                visited += node
                node.predecessors.exists(
                    currentNode ⇒ isTransitivePredecessorInternal(possiblePredecessor, currentNode)
                )
            }
        }
        isTransitivePredecessorInternal(possiblePredecessor, node)
    }

    /**
     * Returns all successors BasicBlocks of a CFGNode
     */
    def getSuccessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        def getSuccessorsInternal(node: CFGNode, visited: Set[CFGNode]): Iterator[BasicBlock] = {
            node.successors.iterator flatMap { currentNode ⇒
                if (currentNode.isBasicBlock)
                    if (visited.contains(currentNode)) None
                    else Some(currentNode.asBasicBlock)
                else getSuccessors(currentNode, visited)
            }
        }
        getSuccessorsInternal(node, visited).toList
    }

    /**
     * Checks if the value written to the field is guaranteed to be always the same.
     * This is true if the value is constant or originates from a deterministic call of a method
     * without non-constant parameters. Alternatively, if the initialization method itself is
     * deterministic and has no parameters, the value is also always the same.
     */
    def checkWriteIsDeterministic(
        origin: Assignment[V],
        method: Method,
        code:   Array[Stmt[V]],
        taCode: TACode[TACMethodParameter, V]
    )(implicit state: State): Boolean = {

        def isConstant(uvar: Expr[V]): Boolean = {
            val defSites = uvar.asVar.definedBy

            def isConstantDef(index: Int) = {
                if (index < 0) false
                else if (code(index).asAssignment.expr.isConst) true
                else {
                    val expr = code(index).asAssignment.expr
                    expr.isFieldRead && (expr.asFieldRead.resolveField(p) match {

                        case Some(field) ⇒
                            state.field == field ||
                                isImmutableReference(propertyStore(field, FieldReferenceImmutability.key))

                        case _ ⇒ false // Unknown field
                    })
                }
            }
            defSites == SelfReferenceParameter || defSites.size == 1 && isConstantDef(defSites.head)
        }

        val value = origin.expr

        def isNonConstDeterministic(
            value:  Expr[V],
            taCode: TACode[TACMethodParameter, V]
        )(implicit state: State): Boolean = {
            (value.astID: @switch) match {
                case BinaryExpr.ASTID ⇒ isConstant(value.asBinaryExpr.left) && isConstant(value.asBinaryExpr.right)

                case GetStatic.ASTID | GetField.ASTID ⇒ value.asFieldRead.resolveField(p) match {
                    case Some(field) ⇒
                        state.field == field ||
                            isImmutableReference(propertyStore(field, FieldReferenceImmutability.key))

                    case _ ⇒ false // Unknown field
                }

                case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                    // If the value originates from a call, that call must be deterministic and may not
                    // have any non constant parameters to guarantee that it is the same on every
                    // invocation. The receiver object must be the 'this' self reference for the same
                    // reason.
                    if (value.asFunctionCall.allParams.forall(isConstant)) {
                        state.lazyInitInvocation = Some((declaredMethods(method), origin.pc))
                        true
                    } else false

                case NewArray.ASTID ⇒ true //TODO look at it

                case Var.ASTID ⇒
                    val varValue = value.asVar
                    varValue.definedBy.size == 1 && //no different values due to different control flows
                        varValue.definedBy.
                        forall(i ⇒ i >= 0 && isNonConstDeterministic(code(i).asAssignment.expr, taCode))

                case New.ASTID ⇒
                    val nonVirtualMethodCallIndexes =
                        origin.asAssignment.targetVar.usedBy.iterator.
                            filter(i ⇒ code(i).isNonVirtualMethodCall)
                    nonVirtualMethodCallIndexes.forall { nonVirtualMethodCallIndex ⇒
                        val callTargetResult =
                            taCode.stmts(nonVirtualMethodCallIndex).asNonVirtualMethodCall.resolveCallTarget(
                                state.field.classFile.thisType
                            )
                        !callTargetResult.isEmpty && (!callTargetResult.value.isConstructor ||
                            //if the constructor is called and it must be deterministic
                            !isNonDeterministic(propertyStore(declaredMethods(callTargetResult.value), Purity.key)))
                    }

                case _ ⇒
                    // The value neither is a constant nor originates from a call, but if the
                    // current method does not take parameters and is deterministic, the value is
                    // guaranteed to be the same on every invocation.
                    lazyInitializerIsDeterministic(method)
            }
        }
        value.isConst || isNonConstDeterministic(value, taCode)
    }

    /**
     * Checks if an expression is a field read of the currently analyzed field.
     * For instance fields, the read must be on the `this` reference.
     */
    def isReadOfCurrentField(
        expr:    Expr[V],
        tacCode: TACode[TACMethodParameter, V],
        index:   Int
    )(implicit state: State): Boolean = {
        def isExprReadOfCurrentField: Int ⇒ Boolean = exprIndex ⇒
            exprIndex == index ||
                exprIndex >= 0 && isReadOfCurrentField(tacCode.stmts(exprIndex).asAssignment.expr, tacCode, exprIndex)
        (expr.astID: @switch) match {
            case GetField.ASTID ⇒
                val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                if (objRefDefinition != SelfReferenceParameter) false
                else expr.asGetField.resolveField(project).contains(state.field)

            case GetStatic.ASTID             ⇒ expr.asGetStatic.resolveField(project).contains(state.field)
            case PrimitiveTypecastExpr.ASTID ⇒ false

            case Compare.ASTID ⇒
                val leftExpr = expr.asCompare.left
                val rightExpr = expr.asCompare.right
                leftExpr.asVar.definedBy.forall(index ⇒
                    index >= 0 && tacCode.stmts(index).asAssignment.expr.isConst) &&
                    rightExpr.asVar.definedBy.forall(isExprReadOfCurrentField) ||
                    rightExpr.asVar.definedBy.forall(index ⇒
                        index >= 0 && tacCode.stmts(index).asAssignment.expr.isConst) &&
                    leftExpr.asVar.definedBy.forall(isExprReadOfCurrentField)

            case VirtualFunctionCall.ASTID ⇒
                val functionCall = expr.asVirtualFunctionCall
                val fieldType = state.field.fieldType
                functionCall.params.isEmpty && (
                    fieldType match {
                        case ObjectType.Byte    ⇒ functionCall.name == "byteValue"
                        case ObjectType.Short   ⇒ functionCall.name == "shortValue"
                        case ObjectType.Integer ⇒ functionCall.name == "intValue"
                        case ObjectType.Long    ⇒ functionCall.name == "longValue"
                        case ObjectType.Float   ⇒ functionCall.name == "floatValue"
                        case ObjectType.Double  ⇒ functionCall.name == "doubleValue"
                        case _                  ⇒ false
                    }
                ) && functionCall.receiver.asVar.definedBy.forall(isExprReadOfCurrentField)

            case _ ⇒ false
        }
    }
    /**
     * Determines if an if-Statement is actually a guard for the current field, i.e. it compares
     * the current field to the default value.
     */
    def isGuard(
        ifStmt:       If[V],
        defaultValue: Any,
        code:         Array[Stmt[V]],
        tacCode:      TACode[TACMethodParameter, V],
        method:       Method
    )(implicit state: State): Boolean = {

        /**
         * Checks if an expression
         */
        @tailrec
        def isDefaultConst(expr: Expr[V]): Boolean = {

            if (expr.isVar) {
                val defSites = expr.asVar.definedBy
                val head = defSites.head
                defSites.size == 1 && head >= 0 && isDefaultConst(code(head).asAssignment.expr)
            } else {
                expr.isIntConst && defaultValue == expr.asIntConst.value || //defaultValue == expr.asIntConst.value ||
                    expr.isFloatConst && defaultValue == expr.asFloatConst.value ||
                    expr.isDoubleConst && defaultValue == expr.asDoubleConst.value ||
                    expr.isLongConst && defaultValue == expr.asLongConst.value ||
                    expr.isNullExpr && defaultValue == null
            }
        }

        /**
         * Checks whether the non-constant expression of the if-Statement is a read of the current
         * field.
         */
        def isGuardInternal(expr: V, tacCode: TACode[TACMethodParameter, V], method: Method): Boolean = {
            expr.definedBy forall { index ⇒
                if (index < 0) false // If the value is from a parameter, this can not be the guard
                else {
                    val isStaticFunctionCall = code(index).asAssignment.expr.isStaticFunctionCall
                    val isVirtualFunctionCall = code(index).asAssignment.expr.isVirtualFunctionCall
                    if (isStaticFunctionCall || isVirtualFunctionCall) {
                        //in case of Integer etc.... .initValue()

                        val calleesResult = propertyStore(declaredMethods(method), Callees.key)
                        if (doCallsIntroduceNonDeterminism(calleesResult, code(index).asAssignment.pc))
                            return false;

                        if (isVirtualFunctionCall) {
                            val virtualFunctionCall = code(index).asAssignment.expr.asVirtualFunctionCall
                            virtualFunctionCall.receiver.asVar.definedBy.forall(receiverDefSite ⇒
                                receiverDefSite >= 0 &&
                                    isReadOfCurrentField(code(receiverDefSite).asAssignment.expr, tacCode, index))
                        } else {
                            isReadOfCurrentField(code(index).asAssignment.expr, tacCode, index)
                        }
                    } else {
                        isReadOfCurrentField(code(index).asAssignment.expr, tacCode, index)
                    }
                }
            }
        }

        //Special handling for these types needed because of compare function in bytecode
        def hasFloatDoubleOrLongType(fieldType: FieldType): Boolean =
            fieldType.isDoubleType || fieldType.isFloatType || fieldType.isLongType

        if (ifStmt.rightExpr.isVar && hasFloatDoubleOrLongType(state.field.fieldType) &&
            ifStmt.rightExpr.asVar.definedBy.head > 0 &&
            tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.isCompare) {

            val left = tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            val right = tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr

            if (leftExpr.isGetField || leftExpr.isGetStatic) isDefaultConst(rightExpr)
            else (rightExpr.isGetField || rightExpr.isGetStatic) && isDefaultConst(leftExpr)

        } else if (ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar && ifStmt.leftExpr.asVar.definedBy.head >= 0 &&
            ifStmt.rightExpr.asVar.definedBy.head >= 0 &&
            hasFloatDoubleOrLongType(state.field.fieldType) && tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).
            asAssignment.expr.isCompare &&
            ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar) {

            val left = tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            val right = tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr

            if (leftExpr.isGetField || leftExpr.isGetStatic) isDefaultConst(rightExpr)
            else (rightExpr.isGetField || rightExpr.isGetStatic) && isDefaultConst(leftExpr)

        } else if (ifStmt.rightExpr.isVar && isDefaultConst(ifStmt.leftExpr)) {
            isGuardInternal(ifStmt.rightExpr.asVar, tacCode, method)
        } else if (ifStmt.leftExpr.isVar && isDefaultConst(ifStmt.rightExpr)) {
            isGuardInternal(ifStmt.leftExpr.asVar, tacCode, method)
        } else false
    }

    /**
     * Checks that the returned value is definitely read from the field.
     */
    def isFieldValueReturned(
        write:        FieldWriteAccessStmt[V],
        writeIndex:   Int,
        readIndex:    Int,
        taCode:       TACode[TACMethodParameter, V],
        guardIndexes: List[(Int, Int, Int, Int)]
    )(implicit state: State): Boolean = {

        def isSimpleReadOfField(expr: Expr[V]) = {
            expr.astID match {
                case GetField.ASTID ⇒
                    val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                    if (objRefDefinition != SelfReferenceParameter)
                        false
                    else
                        expr.asGetField.resolveField(project).contains(state.field)

                case GetStatic.ASTID ⇒
                    expr.asGetStatic.resolveField(project).contains(state.field)

                case _ ⇒ false
            }
        }

        taCode.stmts.forall { stmt ⇒
            !stmt.isReturnValue || {

                val returnValueDefs = stmt.asReturnValue.expr.asVar.definedBy
                val assignedValueDefSite = write.value.asVar.definedBy
                returnValueDefs.forall(_ >= 0) &&
                    {
                        if (returnValueDefs.size == 1 && returnValueDefs.head != readIndex) {
                            val expr = taCode.stmts(returnValueDefs.head).asAssignment.expr
                            isSimpleReadOfField(expr) && guardIndexes.exists {
                                case (_, guardIndex, defaultCase, _) ⇒
                                    dominates(guardIndex, returnValueDefs.head, taCode) &&
                                        (!dominates(defaultCase, returnValueDefs.head, taCode) ||
                                            dominates(writeIndex, returnValueDefs.head, taCode))
                            }
                        } //The field is either read before the guard and returned or
                        // the value assigned to the field is returned
                        else {
                            returnValueDefs.size == 2 && assignedValueDefSite.size == 1 &&
                                returnValueDefs.contains(readIndex) &&
                                {
                                    returnValueDefs.contains(assignedValueDefSite.head) || {
                                        val potentiallyReadIndex = returnValueDefs.filter(_ != readIndex).head
                                        val expr = taCode.stmts(potentiallyReadIndex).asAssignment.expr
                                        isSimpleReadOfField(expr) &&
                                            guardIndexes.exists {
                                                case (_, guardIndex, defaultCase, _) ⇒
                                                    dominates(guardIndex, potentiallyReadIndex, taCode) &&
                                                        (!dominates(defaultCase, returnValueDefs.head, taCode) ||
                                                            dominates(writeIndex, returnValueDefs.head, taCode))
                                            }
                                    }
                                }
                        }
                    }
            }
        }
    }
}
