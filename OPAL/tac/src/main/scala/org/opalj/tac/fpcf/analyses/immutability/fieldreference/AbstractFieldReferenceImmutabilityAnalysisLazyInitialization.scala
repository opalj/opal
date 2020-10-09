/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldreference

import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.NE
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.Method
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
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
     *
     * handles the lazy initialization determination for the write in a given method
     * @author Tobias Roth
     * @return true if we have no thread safe or deterministic lazy initialization
     */
    def handleLazyInitialization(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int],
        tacCai:       TACode[TACMethodParameter, V]
    )(implicit state: State): Boolean = {
        println("handle lazy initialization")
        println(
            s"""
           | field: ${state.field}
           |""".stripMargin
        )
        val lazyInitializationResult: FieldReferenceImmutability =
            determineLazyInitialization(writeIndex, defaultValue, method, code, cfg, tacCai)
        state.referenceImmutability = lazyInitializationResult
        lazyInitializationResult == MutableFieldReference
    }

    /**
     * Determines if a given field is lazy initialized in the given method.
     * @author Tobias Roth
     */
    def determineLazyInitialization(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        tacCode:      TACode[TACMethodParameter, V]
    )(implicit state: State): FieldReferenceImmutability = {
        //println("0")
        val write = code(writeIndex).asFieldWriteAccessStmt
        val writeBB = cfg.bb(writeIndex).asBasicBlock
        val domTree = cfg.dominatorTree
        //println("1")
        val resultCatchesAndThrows = findCaughtsThrowsAndResults(tacCode, cfg)
        //println("2")
        def noInterferingExceptions: Boolean = {
            resultCatchesAndThrows._1.forall(bbCatch ⇒
                resultCatchesAndThrows._2.exists(bbThrow ⇒
                    ((domTree.strictlyDominates(bbCatch._4.nodeId, bbThrow._3.nodeId) || //domination
                        (bbCatch._4 == bbThrow._3 && bbCatch._1 < bbThrow._1)) && // or equal and successor
                        bbThrow._2 == bbCatch._3)))
        }
        // println("3")
        val findGuardResult: (List[(Int, Int, Int, CFGNode, Int, Int)]) = {
            findGuard(method, writeIndex, defaultValue, code, cfg, tacCode)
        }
        //  println("4")
        val (guardedIndex, readIndex, afterGuardRecognizedTheDefaultValueIndex, fieldReadIndex) = {

            if (findGuardResult.nonEmpty)
                (
                    //findGuardResult.head._1,
                    findGuardResult.head._2,
                    findGuardResult.head._3,
                    findGuardResult.head._5,
                    findGuardResult.head._6
                )
            else {
                return MutableFieldReference;
            }
        }
        //println("5")
        val guardedBB = cfg.bb(afterGuardRecognizedTheDefaultValueIndex)
        val elseBB = cfg.bb(guardedIndex)
        //println("6")
        if (isTransitivePredecessor(elseBB, writeBB)) {
            return MutableFieldReference;
        }
        //println("7")
        //prevents that the field is seen with another value
        if (method.returnType == state.field.fieldType && {
            !tacCode.stmts.forall(stmt ⇒ { //TODO
                if (stmt.isReturnValue) {
                    (findGuardResult.exists(x ⇒ {
                        isTransitivePredecessor(
                            cfg.bb(x._2),
                            cfg.bb(tacCode.pcToIndex(stmt.pc))
                        )
                    })
                        || isTransitivePredecessor(writeBB, cfg.bb(tacCode.pcToIndex(stmt.pc))))
                } else
                    true

            })
        }) {
            return MutableFieldReference;
        }
        //println("8")
        val reads = fieldAccessInformation.readAccesses(state.field)
        // println("9")
        if (reads.exists(mAndPCs ⇒ (mAndPCs._1 ne method) && !mAndPCs._1.isInitializer)) {
            return MutableFieldReference;
        }
        // println("B")
        val writes = fieldAccessInformation.writeAccesses(state.field)
        if (writes.exists(x ⇒ ((x._1 eq method) && x._2.size > 1))) {
            return MutableFieldReference;
        }
        // println("C")
        // println (s"field: ${state.field}")
        if (method.returnType == state.field.fieldType &&
            !isTheFieldsValueReturned(write, writeIndex, readIndex, cfg, tacCode)) {
            return MutableFieldReference;
        }
        // println("D")
        //when the method is synchronized the monitor has not to be searched
        if (method.isSynchronized) {
            println("E")
            if (domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId) ||
                (guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex)) {
                // println("F")
                if (noInterferingExceptions) {
                    //  println("G")
                    LazyInitializedThreadSafeFieldReference // result //DCL
                } else {
                    //    println("H")
                    MutableFieldReference
                }
            } else {
                //  println("I")
                MutableFieldReference
            }
        } else {
            //println("else")
            val monitorResult: ((Option[Int], Option[Int]), (Option[CFGNode], Option[CFGNode])) = {
                // println("find monitors start")
                findMonitors(writeIndex, defaultValue, code, cfg, tacCode) //...

            }
            // println("find monitors end")
            if ((monitorResult._1._1.isDefined && monitorResult._1._2.isDefined && monitorResult._2._1.isDefined)
                &&
                (
                    (domTree.strictlyDominates(monitorResult._2._1.get.nodeId, guardedBB.nodeId) ||
                        (monitorResult._2._1.get == guardedBB && monitorResult._1._1.get < afterGuardRecognizedTheDefaultValueIndex)) && //writeIndex)) && //monitor enter dominates guard1
                        ((domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId))
                            || guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex) && //&& //true case dominates Write
                            // The field read must be within the synchronized block
                            (domTree.strictlyDominates(monitorResult._2._1.get.nodeId, cfg.bb(fieldReadIndex).nodeId) ||
                                monitorResult._2._1.get == cfg.bb(fieldReadIndex) && monitorResult._1._1.get < fieldReadIndex)
                )) {
                if (noInterferingExceptions) {
                    //  println("J")
                    LazyInitializedThreadSafeFieldReference // result //DCL
                } else {
                    //  println("K")
                    MutableFieldReference
                }
            } else {
                //println("check write is deterministic: "+checkWriteIsDeterministic(code(write.value.asVar.definedBy.iterator.filter(n ⇒ n >= 0).toList.head).asAssignment, method, code))
                if ((domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId) ||
                    (guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex)) &&
                    write.value.asVar.definedBy.size >= 0 &&
                    (( //IsDeepImmutable //state.field.isFinal ||write.value.asVar.definedBy.head > -1
                        write.value.asVar.definedBy.iterator.filter(n ⇒ n >= 0).toList.nonEmpty &&
                        checkWriteIsDeterministic(code(write.value.asVar.definedBy.iterator.filter(n ⇒ n >= 0).toList.head).asAssignment, method, code)
                    ))) {
                    //   println("L")
                    if (noInterferingExceptions) {
                        if (state.field.fieldType.computationalType != ComputationalTypeInt &&
                            state.field.fieldType.computationalType != ComputationalTypeFloat) {

                            LazyInitializedNotThreadSafeFieldReference
                        } else
                            LazyInitializedNotThreadSafeButDeterministicFieldReference
                    } else {
                        //    println("M")
                        MutableFieldReference
                    }
                } else {
                    //  println("N")
                    MutableFieldReference
                }
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
     *         The third element of the triple returns:
     * @author Tobias Roth
     */
    def findCaughtsThrowsAndResults(
        tacCode: TACode[TACMethodParameter, V],
        cfg:     CFG[Stmt[V], TACStmts[V]]
    ): (List[(Int, ObjectType, IntTrieSet, CFGNode)], List[(Int, IntTrieSet, CFGNode)]) = { //}, List[CFGNode]) = {
        var caughtExceptions: List[(Int, ObjectType, IntTrieSet, CFGNode)] = List.empty
        var throwStatements: List[(Int, IntTrieSet, CFGNode)] = List.empty
        //var returnNodes: List[CFGNode] = List.empty
        for (stmt ← tacCode.stmts) {
            if (!stmt.isNop) { //pc < ...
                val currentBB = cfg.bb(tacCode.pcToIndex(stmt.pc))
                (stmt.astID: @switch) match {
                    case CaughtException.ASTID ⇒
                        val caughtException = stmt.asCaughtException
                        val exceptionType =
                            if (caughtException.exceptionType.isDefined) {
                                val intermediateExceptionType = caughtException.exceptionType.get
                                //if (intermediateExceptionType.isObjectType)
                                intermediateExceptionType.asObjectType
                                //else
                                //ObjectType.Throwable //   ObjectType.Exception
                            } else
                                ObjectType.Throwable // ObjectType.Exception
                        caughtExceptions =
                            ((caughtException.pc, exceptionType, caughtException.origins, currentBB)) :: caughtExceptions
                    case Throw.ASTID ⇒
                        val throwStatement = stmt.asThrow
                        val throwStatementDefinedBys =
                            // if (throwStatement.exception.isVar) {
                            throwStatement.exception.asVar.definedBy
                        // } else
                        //    IntTrieSet.empty
                        throwStatements = ((throwStatement.pc, throwStatementDefinedBys, currentBB)) :: throwStatements
                    //case ReturnValue.ASTID ⇒
                    //    returnNodes = (currentBB) :: returnNodes
                    case _ ⇒
                }
                //}
            }
        }
        (caughtExceptions, throwStatements) //, returnNodes)
    }

    def findMonitors(
        fieldWrite:   Int,
        defaultValue: Any,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        tacCode:      TACode[TACMethodParameter, V]
    )(implicit state: State): ((Option[Int], Option[Int]), (Option[CFGNode], Option[CFGNode])) = {

        var result: (Option[Int], Option[Int]) = (None, None)
        var dclEnterBBs: List[CFGNode] = List.empty
        var dclExitBBs: List[CFGNode] = List.empty
        val startBB = cfg.bb(fieldWrite)
        var monitorExitQueuedBBs: Set[CFGNode] = startBB.successors
        var worklistMonitorExit = getSuccessors(startBB, Set.empty)

        def checkMonitor(v: V)(implicit state: State): Boolean = {
            v.definedBy
                .forall(definedByIndex ⇒ {
                    if (definedByIndex >= 0) {
                        val stmt = tacCode.stmts(definedByIndex)
                        stmt match {
                            case Assignment(_, DVar(_, _), classConst: ClassConst) ⇒ {
                                state.field.classFile.thisType == classConst.value ||
                                    state.field.fieldType == classConst.value
                            }
                            case Assignment(
                                _,
                                DVar(_, _),
                                GetField(_, _, _,
                                    classType,
                                    UVar(_, _))
                                ) ⇒
                                classType ==
                                    state.field.classFile.thisType
                            case _ ⇒ false
                        }
                    } else // (definedByIndex <= -1)
                        true
                })
        }

        var monitorEnterQueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklistMonitorEnter = getPredecessors(startBB, Set.empty)
        // println("find monitorenter")
        //find monitorenter
        while (worklistMonitorEnter.nonEmpty) {
            val curBB = worklistMonitorEnter.head
            //  println("curBB: "+curBB)
            worklistMonitorEnter = worklistMonitorEnter.tail
            val startPC = curBB.startPC
            val endPC = curBB.endPC
            var flag = true
            for (i ← startPC to endPC) {
                (code(i).astID: @switch) match {
                    case MonitorEnter.ASTID ⇒
                        val me = code(i).asMonitorEnter
                        if (checkMonitor(me.objRef.asVar)) { //me.pc, , curBB
                            result = (Some(tacCode.pcToIndex(me.pc)), (result._2))
                            dclEnterBBs = curBB :: dclEnterBBs
                            flag = false
                        }
                    case _ ⇒
                }
            }
            if (flag) {
                val predecessor = getPredecessors(curBB, monitorEnterQueuedBBs)
                worklistMonitorEnter ++= predecessor
                monitorEnterQueuedBBs ++= predecessor
            }
        }
        //println("find monitorexit")
        //find monitorexit
        while (worklistMonitorExit.nonEmpty) {
            val curBB = worklistMonitorExit.head

            //  println("curBB: "+curBB)
            worklistMonitorExit = worklistMonitorExit.tail
            val endPC = curBB.endPC

            val cfStmt = code(endPC)
            (cfStmt.astID: @switch) match {
                case MonitorExit.ASTID ⇒
                    val mex = cfStmt.asMonitorExit
                    if (checkMonitor(mex.objRef.asVar)) {
                        result = ((result._1), Some(tacCode.pcToIndex(mex.pc)))
                        dclExitBBs = curBB :: dclExitBBs
                    }
                case _ ⇒
                    val successors = getSuccessors(curBB, monitorExitQueuedBBs)
                    worklistMonitorExit ++= successors
                    monitorExitQueuedBBs ++= successors
            }
        }

        val bbsEnter = {
            if (dclEnterBBs.nonEmpty)
                Some(dclEnterBBs.head)
            else None
        }
        val bbsExit = {
            if (dclExitBBs.nonEmpty)
                Some(dclExitBBs.head)
            else None
        }
        (result, (bbsEnter, bbsExit))
    }

    /**
     * Finds the index of the guarding if-Statement for a lazy initialization, the index of the
     * first statement executed if the field does not have its default value and the index of the
     * field read used for the guard and the index of the fieldread.
     */
    // var savedGuards: mutable.HashMap[(Int, Method), ((Option[(Int, Int, Int, CFGNode, Int)]))] =
    //     new mutable.HashMap[(Int, Method), ((Option[(Int, Int, Int, CFGNode, Int)]))]()

    def findGuard(
        method:       Method,
        fieldWrite:   Int,
        defaultValue: Any,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        tacCode:      TACode[TACMethodParameter, V]
    )(implicit state: State): (List[(Int, Int, Int, CFGNode, Int, Int)]) = {

        // println(s"start findguard defaultValue: $defaultValue")
        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklist: List[BasicBlock] = getPredecessors(startBB, Set.empty).toList
        var seen: Set[BasicBlock] = Set.empty
        var result: List[(Int, Int, CFGNode, Int)] = List.empty //None

        //  println("startBB: "+startBB)

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail
            if (!seen.contains(curBB)) {
                seen += curBB

                // println("curBB: "+curBB)

                //val startPC = curBB.startPC
                val endPC = curBB.endPC

                val cfStmt = code(endPC)
                (cfStmt.astID: @switch) match {
                    case If.ASTID ⇒

                        // println("ifstmt: ")
                        // println(cfStmt)
                        val ifStmt = cfStmt.asIf
                        //ifStmt.condition match {
                        // println("AA")
                        /*  println(s"isguard: ${
                        isGuard(
                            ifStmt,
                            defaultValue,
                            code,
                            tacCode
                        )
                    }") */
                        if (ifStmt.condition.equals(EQ) && curBB != startBB && isGuard(
                            ifStmt,
                            defaultValue,
                            code,
                            tacCode,
                            method
                        )) {
                            // println("BB")

                            //case EQ
                            //if =>
                            ///if (result.size > 0) { //.isDefined) {
                            ///    if (result.head._1 != endPC || result.head._2 != endPC + 1) {} //return result //None;
                            ///} else {
                            result = (endPC, endPC + 1, curBB, ifStmt.targetStmt) :: result
                            ///}
                        } else if (ifStmt.condition.equals(NE) && curBB != startBB && isGuard(
                            ifStmt,
                            defaultValue,
                            code,
                            tacCode,
                            method
                        )) {
                            //    println("CC")
                            //case NE
                            //if =>
                            ///if (result.size > 0) { //.isDefined) {
                            ///     if (result.head._1 != endPC || result.head._2 != ifStmt.targetStmt)
                            ///        result //None
                            ///} else {
                            result = (endPC, ifStmt.targetStmt, curBB, endPC + 1) :: result
                            ///}
                        } else {
                            //  println("DD")
                            // Otherwise, we have to ensure that a guard is present for all predecessors
                            //case _ =>
                            //if (startPC == 0) result //None;

                            //isTransitivePredecessor(cfg.bb(fieldWrite), cfg.bb(ifStmt.target), Set.empty)

                            //fieldWrite != ifStmt.target
                            //
                            // println("befor transitive call...")
                            if ((cfg.bb(fieldWrite) != cfg.bb(ifStmt.target) || fieldWrite < ifStmt.target) &&
                                isTransitivePredecessor(cfg.bb(fieldWrite), cfg.bb(ifStmt.target))) //(ifStmt.target > fieldWrite)
                                return List.empty //in cases where other if-statements destroy
                        }
                        //}
                        //}
                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors

                    // Otherwise, we have to ensure that a guard is present for all predecessors
                    case _ ⇒
                        ///if (startPC == 0) result //None;

                        val predecessors = getPredecessors(curBB, enqueuedBBs)
                        worklist ++= predecessors
                        enqueuedBBs ++= predecessors
                }
            }

        }

        var finalResult: List[(Int, Int, Int, CFGNode, Int, Int)] = List.empty
        var fieldReadIndex = 0
        result.foreach(result ⇒ {
            // The field read that defines the value checked by the guard must be used only for the
            // guard or directly if the field's value was not the default value
            val ifStmt = code(result._1).asIf
            val expr =
                if (ifStmt.leftExpr.isConst)
                    ifStmt.rightExpr
                else
                    ifStmt.leftExpr
            val definitions = expr.asVar.definedBy
            if (definitions.exists(_ < 0)) //.head < 0)
                return finalResult;
            println(
                s"""definitions:
                   |field read
                   | ${code(definitions.head).asAssignment.expr}
                   |
                   |
                   |""".stripMargin
            )
            fieldReadIndex = definitions.head
            val fieldReadUses = code(definitions.head).asAssignment.targetVar.usedBy //TODO ...
            val fieldReadUsedCorrectly =
                fieldReadUses.forall(use ⇒ use == result._1 || use == result._2)

            if (definitions.size == 1 && definitions.head >= 0 && fieldReadUsedCorrectly)
                finalResult = (result._1, result._2, definitions.head, result._3, result._4, fieldReadIndex) :: finalResult // Found proper guard
            else finalResult
        }) // else None)
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
        var visited: Set[CFGNode] = Set.empty
        def isTransitivePredecessorInternal(possiblePredecessor: CFGNode, node: CFGNode): Boolean = {
            /*println(
                s"""
               |is transitive predecessor:
               |possiblePredecessor: $possiblePredecessor
               | node: $node
               | visited: ${visited.mkString("\n")}
               |""".stripMargin
            ) */

            if (possiblePredecessor == node) {
                true
            } else if (visited.contains(node))
                false
            else {
                visited += node
                //val predecessors = node.predecessors //getPredecessors(node, Set.empty)
                //val tmpVisited2 = tmpVisited ++ predecessors
                //println("predecessors: "+predecessors.mkString(", \n"))
                //println("tmpVisited2: "+tmpVisited2)
                node.predecessors.exists(
                    currentNode ⇒ {
                        isTransitivePredecessorInternal(possiblePredecessor, currentNode)
                    }
                )
            }
        }
        println("--------------------------------------------------------------------------------------------------------")
        isTransitivePredecessorInternal(possiblePredecessor, node)
    }

    /**
     * Returns all successors BasicBlocks of a CFGNode
     */
    def getSuccessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        def getSuccessorsInternal(node: CFGNode, visited: Set[CFGNode]): Iterator[BasicBlock] = {
            node.successors.iterator flatMap ({ currentNode ⇒
                if (currentNode.isBasicBlock)
                    if (visited.contains(currentNode)) None
                    else Some(currentNode.asBasicBlock)
                else getSuccessors(currentNode, visited)
            })
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
        code:   Array[Stmt[V]]
    )(implicit state: State): Boolean = {

        def isConstant(uvar: Expr[V]): Boolean = {
            val defSites = uvar.asVar.definedBy

            def isConstantDef(index: Int) = {
                if (index < 0)
                    false
                else if (code(index).asAssignment.expr.isConst)
                    true
                else {
                    val expr = code(index).asAssignment.expr
                    expr.isFieldRead && (expr.asFieldRead.resolveField(p) match {
                        case Some(field) ⇒
                            state.field == field || isImmutableReference(propertyStore(field, FieldReferenceImmutability.key))
                        case _ ⇒ // Unknown field
                            false
                    })
                }
            }
            val result = defSites == SelfReferenceParameter ||
                defSites.size == 1 && isConstantDef(defSites.head)
            result
        }

        val value = origin.expr

        def isNonConstDeterministic(value: Expr[V]): Boolean = {
            (value.astID: @switch) match {
                case BinaryExpr.ASTID ⇒
                    isConstant(value.asBinaryExpr.left) &&
                        isConstant(value.asBinaryExpr.right)
                case GetStatic.ASTID | GetField.ASTID ⇒
                    value.asFieldRead.resolveField(p) match {
                        case Some(field) ⇒
                            state.field == field || isImmutableReference(propertyStore(field, FieldReferenceImmutability.key))
                        case _ ⇒ // Unknown field
                            false
                    }
                case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                    // If the value originates from a call, that call must be deterministic and may not
                    // have any non constant parameters to guarantee that it is the same on every
                    // invocation. The receiver object must be the 'this' self reference for the same
                    // reason.
                    if (value.asFunctionCall.allParams.forall(isConstant(_))) {
                        state.lazyInitInvocation = Some((declaredMethods(method), origin.pc))
                        true
                    } else
                        false
                case NewArray.ASTID ⇒
                    true //TODO look at it
                case Var.ASTID ⇒ {
                    val varValue = value.asVar
                    varValue.definedBy.size == 1 && //stronger requirement -> only one definition side
                        varValue.definedBy.forall(i ⇒ i >= 0 && isNonConstDeterministic(code(i).asAssignment.expr))
                }
                case New.ASTID ⇒ {
                    //TODO constructor must be deterministic
                    //TODO check that the nonvirtualmethod call calls the constructor
                    //TODO check
                    val nonVirtualMethodCallIndex =
                        origin.asAssignment.targetVar.usedBy.iterator.filter(i ⇒ code(i).isNonVirtualMethodCall).toList.head
                    val resultCallees = propertyStore(declaredMethods(method), Callees.key)
                    !handleCalls(resultCallees, nonVirtualMethodCallIndex) &&
                        code(nonVirtualMethodCallIndex).asNonVirtualMethodCall.params.forall(isConstant)
                }
                case _ ⇒
                    // The value neither is a constant nor originates from a call, but if the
                    // current method does not take parameters and is deterministic, the value is
                    // guaranteed to be the same on every invocation.
                    lazyInitializerIsDeterministic(method, code)
            }
        }
        val result = value.isConst || isNonConstDeterministic(value)

        result
    }

    /**
     * Checks if an expression is a field read of the currently analyzed field.
     * For instance fields, the read must be on the `this` reference.
     */
    def isReadOfCurrentField(expr: Expr[V], tacCode: TACode[TACMethodParameter, V])(implicit state: State): Boolean = {
        var seenExpressions: Set[Expr[V]] = Set.empty
        def isReadOfCurrentFieldInternal(expr: Expr[V], tacCode: TACode[TACMethodParameter, V])(implicit state: State): Boolean = {

            if (seenExpressions.contains(expr)) {
                return false
            };
            seenExpressions += expr
            def isExprReadOfCurrentField(pc: Int): Int ⇒ Boolean = index ⇒
                index == tacCode.pcToIndex(pc) ||
                    index >= 0 &&
                    isReadOfCurrentFieldInternal(tacCode.stmts(index).asAssignment.expr, tacCode)
            (expr.astID: @switch) match {
                case GetField.ASTID ⇒

                    //f (state.field.classFile.thisType.simpleName.contains("DoubleCheckedLocking")) {
                    /*println(
                        s"""
                             | field: ${state.field}
                             | expr: $expr
                             | objectDefinition: ${expr.asGetField.objRef.asVar.definedBy}
                             | SelfReferenceParameter: ${SelfReferenceParameter}
                             | resolve: ${if (expr.asGetField.objRef.asVar.definedBy == SelfReferenceParameter) expr.asGetField.resolveField(project)}
                             | field: ${state.field}
                             |""".stripMargin
                    ) */
                    //}
                    val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                    if (objRefDefinition != SelfReferenceParameter) false
                    else expr.asGetField.resolveField(project).contains(state.field)
                case GetStatic.ASTID ⇒ expr.asGetStatic.resolveField(project).contains(state.field)
                case Compare.ASTID ⇒ {
                    val leftExpr = expr.asCompare.left
                    val rightExpr = expr.asCompare.right

                    leftExpr.asVar.definedBy.forall(isExprReadOfCurrentField(expr.asCompare.pc)) ||
                        rightExpr.asVar.definedBy.forall(isExprReadOfCurrentField(expr.asCompare.pc))
                    //TODO
                    //TODO check if sufficient
                    //TODO check if handling of methods needed
                    //TODO check if semantic is correct
                    //TODO check if handling of method arguments needed
                }
                case VirtualFunctionCall.ASTID
                    | NonVirtualFunctionCall.ASTID
                    | StaticFunctionCall.ASTID ⇒ {
                    // val (receiverDefSites,
                    val (params, pc) =
                        if (expr.isVirtualFunctionCall) {
                            val virtualFunctionCall = expr.asVirtualFunctionCall
                            //(virtualFunctionCall.receiver.asVar.definedBy,
                            (virtualFunctionCall.params, virtualFunctionCall.pc)
                        } else if (expr.isStaticFunctionCall) {
                            val staticFunctionCall = expr.asStaticFunctionCall
                            //val receiverDefSites = staticFunctionCall.receiverOption
                            //if (receiverDefSites.isEmpty)
                            //    return false;
                            //(receiverDefSites.get.asVar.definedBy,
                            (staticFunctionCall.params, staticFunctionCall.pc)
                        } else {
                            val nonVirtualFunctionCall = expr.asNonVirtualFunctionCall
                            //(nonVirtualFunctionCall.receiver.asVar.definedBy,
                            (nonVirtualFunctionCall.params, nonVirtualFunctionCall.pc)
                        }
                    params.forall(expr ⇒
                        expr.asVar.definedBy.forall(isExprReadOfCurrentField(pc)))
                    //isReadOfCurrentFieldInternal(expr, tacCode))
                }
                case _ ⇒ false
            }
        }
        isReadOfCurrentFieldInternal(expr, tacCode)
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
        import org.opalj.br.FieldType
        import scala.annotation.tailrec

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
                /*
                println(
                    s"""
                     | isIntConst: ${expr.isIntConst}
                     | defaultValue: $defaultValue
                     | expr: ${expr.asIntConst.value}
                     | isFloatConst: ${expr.isFloatConst}
                     | isDoublConst: ${expr.isDoubleConst}
                     | isLongConst: ${expr.isLongConst}
                     | isNullExpr: ${expr.isNullExpr}
                     | expr.isIntConst && defaultValue == expr.asIntConst.value: ${expr.isIntConst && defaultValue == expr.asIntConst.value}
                     |""".stripMargin
                )*/

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
            //  println("i1")
            expr.definedBy forall { index ⇒
                //   println("index: "+index)
                if (index < 0)
                    false // If the value is from a parameter, this can not be the guard
                else {
                    val isStaticFunctionCall = code(index).asAssignment.expr.isStaticFunctionCall
                    val isVirtualFunctionCall = code(index).asAssignment.expr.isVirtualFunctionCall

                    if (isStaticFunctionCall || isVirtualFunctionCall) {

                        //in case of Integer etc.... .initValue()

                        val calleesResult = propertyStore(declaredMethods(method), Callees.key)

                        if (handleCalls(calleesResult, code(index).asAssignment.pc))
                            return false;

                        if (isVirtualFunctionCall) {
                            val virtualFunctionCall = code(index).asAssignment.expr.asVirtualFunctionCall
                            virtualFunctionCall.receiver.asVar.definedBy.forall(receiverDefSite ⇒
                                receiverDefSite >= 0 && isReadOfCurrentField(code(receiverDefSite).asAssignment.expr, tacCode))
                        } else { //if(isStaticFunctionCall){
                            //val staticFunctionCall = code(index).asAssignment.expr.asStaticFunctionCall
                            //staticFunctionCall.receiverOption.
                            isReadOfCurrentField(code(index).asAssignment.expr, tacCode)
                        }
                    } else {
                        method.asMethod.isVirtualCallTarget
                        isReadOfCurrentField(code(index).asAssignment.expr, tacCode)
                    }
                }
            }
        }

        def isFloatDoubleOrLong(fieldType: FieldType): Boolean =
            fieldType.isDoubleType || fieldType.isFloatType || fieldType.isLongType

        if (ifStmt.rightExpr.isVar && isFloatDoubleOrLong(state.field.fieldType) && ifStmt.rightExpr.asVar.definedBy.head > 0 && tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.isCompare) {
            //ifStmt.leftExpr.isIntConst
            //ifStmt.leftExpr.asIntConst.value==0
            // println("1")
            val left = tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            // println("2")
            val right = tacCode.stmts(ifStmt.rightExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            // println("3")
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            // println("4")
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr
            // println("5")
            if (leftExpr.isGetField) {
                //  println("6")
                val result = isDefaultConst(rightExpr)
                // println("result: "+result)
                result
            } else if (rightExpr.isGetField) {
                // println("7")
                val result = isDefaultConst(leftExpr)
                // println("result: "+result)
                result
            } else { false } //TODO reasoning
        } else if (ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar && ifStmt.leftExpr.asVar.definedBy.head >= 0 &&
            ifStmt.rightExpr.asVar.definedBy.head >= 0 &&
            isFloatDoubleOrLong(state.field.fieldType) && tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).
            asAssignment.expr.isCompare &&
            ifStmt.leftExpr.isVar && ifStmt.rightExpr.isVar) {
            //  println("9")
            val left = tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.left.asVar
            val right = tacCode.stmts(ifStmt.leftExpr.asVar.definedBy.head).asAssignment.expr.asCompare.right.asVar
            val leftExpr = tacCode.stmts(left.definedBy.head).asAssignment.expr
            val rightExpr = tacCode.stmts(right.definedBy.head).asAssignment.expr
            if (leftExpr.isGetField) {
                val result = isDefaultConst(rightExpr)
                result
            } else if (rightExpr.isGetField) {
                isDefaultConst(leftExpr)
            } else false //TODO reasoning
        } else if ((ifStmt.rightExpr.isVar) && isDefaultConst(ifStmt.leftExpr)) {
            // println("12")
            isGuardInternal(ifStmt.rightExpr.asVar, tacCode, method)
        } else if (ifStmt.leftExpr.isVar && isDefaultConst(ifStmt.rightExpr)) {
            // println("13")
            val result = isGuardInternal(ifStmt.leftExpr.asVar, tacCode, method)
            // println(s"result: $result")
            result
        } else {
            // println("14")
            false
        }
    }

    /**
     *
     * Checks that the value of the field is returned.
     *
     */
    def isTheFieldsValueReturned(
        write:      FieldWriteAccessStmt[V],
        writeIndex: Int,
        readIndex:  Int,
        cfg:        CFG[Stmt[V], TACStmts[V]],
        tacCode:    TACode[TACMethodParameter, V]
    )(implicit state: State): Boolean = {
        val startBB = cfg.bb(0).asBasicBlock
        //var curBB = startBB
        var queuedNodes: Set[CFGNode] = Set.empty
        var workList = getSuccessors(startBB, queuedNodes)
        workList ++= Set(startBB)
        var tmp = -1
        while (workList.nonEmpty) {
            val currentBB = workList.head
            workList = workList.tail
            val startPC = {
                if (currentBB == startBB)
                    writeIndex + 1
                else
                    currentBB.startPC
            }
            val endPC = currentBB.endPC
            var pc = startPC
            while (pc <= endPC) {
                val index = pc
                val stmt = tacCode.stmts(index)
                if (stmt.isAssignment) {
                    val assignment = stmt.asAssignment
                    if (isReadOfCurrentField(assignment.expr, tacCode)) {
                        tmp = pc
                    }
                } else if (stmt.isReturnValue) {
                    val returnValueDefs = stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValueDefs.size == 2 &&
                        returnValueDefs.contains(write.value.asVar.definedBy.head) &&
                        returnValueDefs.contains(readIndex)) {
                        return true;
                    } // direct return of the written value
                    else if (tmp >= 0 && (returnValueDefs == IntTrieSet(tmp) ||
                        returnValueDefs == IntTrieSet(readIndex, tmp))) {
                        return true;
                    } // return of field value loaded by field read
                    else {
                        return false;
                    } // return of different value
                } else {

                }
                pc = pc + 1
            }
            val successors = getSuccessors(currentBB, queuedNodes)
            workList ++= successors
            queuedNodes ++= successors
        }
        false
    }

}
