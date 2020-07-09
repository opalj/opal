/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.immutability.reference

import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.NE
import org.opalj.ai.isImmediateVMException
import org.opalj.ai.pcOfImmediateVMException
import org.opalj.ai.pcOfMethodExternalException
import org.opalj.br.ComputationalTypeFloat
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.Assignment
import org.opalj.tac.CaughtException
import org.opalj.tac.ClassConst
import org.opalj.tac.DVar
import org.opalj.tac.Expr
import org.opalj.tac.FieldWriteAccessStmt
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.If
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACStmts
import org.opalj.tac.TACode
import org.opalj.tac.UVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.value.ValueInformation

import scala.annotation.switch

trait AbstractReferenceImmutabilityAnalysisLazyInitialization
    extends AbstractReferenceImmutabilityAnalysis
    with FPCFAnalysis {

    /**
     * Checks whether a field write may be a lazy initialization.
     *
     * We only consider simple cases of lazy initialization where the single field write is guarded
     * so it is executed only if the field still has its default value and where the written value
     * is guaranteed to be the same even if the write is executed multiple times (may happen because
     * of the initializing method being executed more than once on concurrent threads).
     *
     * Also, all field reads must be used only for a guard or their uses have to be guarded
     * themselves.
     */
    def isLazyInitialization(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int]
    )(implicit state: State): Boolean = {
        val write = code(writeIndex).asFieldWriteAccessStmt
        //println("0001")
        if (state.field.fieldType.computationalType != ComputationalTypeInt &&
            state.field.fieldType.computationalType != ComputationalTypeFloat) {
            // Only handle lazy initialization of ints and floats as they are guaranteed to be
            // written atomically
            return false;
        }
        //println("0002")
        //--------------------------------------------------------
        //TODO reasoning if there is another way to do this
        val writes = fieldAccessInformation.writeAccesses(state.field)
        //println("writes: "+writes)
        if (writes.exists(x ⇒ ((x._1 eq method) && x._2.size > 1))) //filter(mAndPCs ⇒ (mAndPCs._1 eq method))
            return false; // more than one write in the method
        //----------------------------------------------------------------
        val reads = fieldAccessInformation.readAccesses(state.field)
        if (reads.exists(mAndPCs ⇒ (mAndPCs._1 ne method) && !mAndPCs._1.isInitializer)) {
            return false; // Reads outside the (single) lazy initialization method
        }
        //println("0003")
        // There must be a guarding if-Statement
        // The guardIndex is the index of the if-Statement, the guardedIndex is the index of the
        // first statement that is executed after the if-Statement if the field's value was not the
        // default value
        val (guardIndex, guardedIndex, readIndex) = {
            val findGuardResult = findGuard(writeIndex, defaultValue, code, cfg)
            //println("find guard result: "+findGuardResult)
            if (findGuardResult.isDefined)
                (findGuardResult.get._1, findGuardResult.get._2, findGuardResult.get._3)
            else return false;
            /**
             * findGuardResult match {
             * case Some((guard, guarded, read)) ⇒ (guard, guarded, read)
             * case None                         ⇒ return false;
             * } *
             */
        }

        //println("0004")
        // Detect only simple patterns where the lazily initialized value is returned immediately
        if (!checkImmediateReturn(write, writeIndex, readIndex, code))
            return false;
        //println("0005")
        // The value written must be computed deterministically and the writes guarded correctly
        if (!checkWrites(write, writeIndex, guardIndex, guardedIndex, method, code, cfg))
            return false;
        //println("0006")
        // Field reads (except for the guard) may only be executed if the field's value is not the
        // default value
        if (!checkReads(reads, readIndex, guardedIndex, writeIndex, cfg, pcToIndex))
            return false;
        //println("0007")
        true
    }

    def isThreadSafeLazyInitialisation(
        writeIndex:   Int,
        defaultValue: Any,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int],
        tacCai:       TACode[TACMethodParameter, V]
    )(implicit state: State): ReferenceImmutability = {
        var result: ReferenceImmutability = LazyInitializedThreadSafeReference
        val write = code(writeIndex).asFieldWriteAccessStmt
        val writeBB = cfg.bb(writeIndex).asBasicBlock
        val domTree = cfg.dominatorTree

        val (guardIndex, guardedIndex, readIndex, afterGuardRecognizedTheDefaultValueIndex) = {
            val findGuardResult: (Option[(Int, Int, Int, CFGNode, Int)]) = findGuard(writeIndex, defaultValue, code, cfg)
            if (findGuardResult.isDefined)
                (findGuardResult.get._1, findGuardResult.get._2, findGuardResult.get._3, findGuardResult.get._5)
            else return MutableReference;
        }
        val guardedBB = cfg.bb(afterGuardRecognizedTheDefaultValueIndex)

        val monitorResult: ((Option[Int], Option[Int]), (Option[CFGNode], Option[CFGNode])) =
            findMonitor(writeIndex, defaultValue, code, cfg, tacCai)

        val reads = fieldAccessInformation.readAccesses(state.field)
        if (reads.exists(mAndPCs ⇒ (mAndPCs._1 ne method) && !mAndPCs._1.isInitializer)) {
            return MutableReference
        }
        val writes = fieldAccessInformation.writeAccesses(state.field)
        if (writes.exists(x ⇒ ((x._1 eq method) && x._2.size > 1))) {
            return MutableReference
        };
        if (method.returnType == state.field.fieldType &&
            !checkThatTheValueOfTheFieldIsReturned(write, writeIndex, readIndex, code)) {
            return MutableReference;
        }

        result = checkWriteIsGuarded2(writeIndex, guardIndex, guardedIndex, method, code, cfg)
        /*
        println(
            s""" method is synchronized: ${method.isSynchronized}
               | (domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId) : ${(domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId))}
               | (guardedBB == writeBB: ${guardedBB == writeBB}
               |  writeIndex: $writeIndex
               |""".stripMargin
        ) */

        if ((monitorResult._1._1.isDefined && monitorResult._1._2.isDefined && monitorResult._2._1.isDefined)
            &&
            (
                (domTree.strictlyDominates(monitorResult._2._1.get.nodeId, guardedBB.nodeId) ||
                    (monitorResult._2._1.get == guardedBB && monitorResult._1._1.get < afterGuardRecognizedTheDefaultValueIndex)) && //writeIndex)) && //monitor enter dominates guard1
                    ((domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId))
                        || guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex) //&& //true case dominates Write
            )) {

            return LazyInitializedThreadSafeReference // result //DCL
        } /*else if ((domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId))
            || guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex) {
            LazyInitializedNotThreadSafeOrNotDeterministicReference
        }*/ else if (domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId) || (guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex)) {
            if (method.isSynchronized) {
                return LazyInitializedThreadSafeReference
                // result
            } else {
                LazyInitializedNotThreadSafeOrNotDeterministicReference
            }
            //TODO !! reasoning
            /*if (writes.exists(x ⇒ ((x._1 eq method) && x._2.size > 1))) {
                return MutableReference
            } else {
                return LazyInitializedNotThreadSafeOrNotDeterministicReference
            }*/
        } else if (((domTree.strictlyDominates(guardedBB.nodeId, writeBB.nodeId))
            || guardedBB == writeBB && afterGuardRecognizedTheDefaultValueIndex < writeIndex)) {
            return LazyInitializedNotThreadSafeOrNotDeterministicReference
        } //only guard{
        else {
            return MutableReference
        }

    }

    def findMonitor(
        fieldWrite:   Int,
        defaultValue: Any,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        tacCode:      TACode[TACMethodParameter, V]
    )(implicit state: State): ((Option[Int], Option[Int]), (Option[CFGNode], Option[CFGNode])) = { //(implicit state: State)

        var result: (Option[Int], Option[Int]) = (None, None)
        var dclEnterBBs: List[CFGNode] = List.empty
        var dclExitBBs: List[CFGNode] = List.empty
        val startBB = cfg.bb(fieldWrite)
        var MonitorExitqueuedBBs: Set[CFGNode] = startBB.successors
        var worklistMonitorExit = getSuccessors(startBB, Set.empty)

        def checkMonitor(pc: PC, v: UVar[ValueInformation], curBB: CFGNode)(implicit state: State): Boolean = {
            v.defSites.filter(i ⇒ {
                //   println("current i: "+i)
                if (i > 0) {
                    val stmt = tacCode.stmts(i)
                    //     println("stmt: "+stmt)
                    stmt match {
                        case Assignment(pc1, DVar(useSites, value), cc @ ClassConst(_, constant)) ⇒ {
                            state.field.classFile.thisType == cc.value || state.field.fieldType == cc.value
                        }
                        case Assignment(pc1, DVar(value1, defSites1), GetField(pc2, t, name, classType, UVar(value2, defSites2))) ⇒
                            classType ==
                                state.field.
                                classFile.thisType
                        //&& name == state.field.name
                        case _ ⇒ false
                    }
                } else // (i == -1)
                    true

            }).size == v.defSites.size
        }

        var monitorEnterqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklistMonitorEnter = getPredecessors(startBB, Set.empty)
        //find monitorenter
        while (!worklistMonitorEnter.isEmpty) {
            val curBB = worklistMonitorEnter.head
            //  println("curBB: "+curBB)
            worklistMonitorEnter = worklistMonitorEnter.tail
            //val startPC = curBB.startPC
            //val endPC = curBB.endPC
            val startPC = curBB.startPC
            val endPC = curBB.endPC
            //val cfStmt = code(startPC) //(endPC)
            //   println("cfg should be monitor enter: "+cfStmt)

            var flag = true
            for (i ← startPC to endPC) {
                code(i) match {
                    case me @ MonitorEnter(pc, v @ UVar(defSites, value)) //if(v.value.computationalType == state.field.fieldType)
                    ⇒ {
                        if (checkMonitor(pc, v, curBB)) {
                            result = (Some(tacCode.pcToIndex(pc)), (result._2))
                            dclEnterBBs = curBB :: dclEnterBBs
                            flag = false
                        }
                    }
                    case _ ⇒
                }
            }
            if (flag) {
                val predecessor = getPredecessors(curBB, monitorEnterqueuedBBs)
                worklistMonitorEnter ++= predecessor
                monitorEnterqueuedBBs ++= predecessor
            }
        }

        //find monitorexit
        while (!worklistMonitorExit.isEmpty) {
            val curBB = worklistMonitorExit.head
            worklistMonitorExit = worklistMonitorExit.tail
            //val startPC = curBB.startPC
            val endPC = curBB.endPC
            val cfStmt = code(endPC)
            cfStmt match {
                case MonitorExit(pc, v @ UVar(defSites, value)) //if(v.value.computationalType == state.field.fieldType)
                ⇒
                    if (checkMonitor(pc, v, curBB)) {
                        result = ((result._1), Some(tacCode.pcToIndex(pc)))
                        dclExitBBs = curBB :: dclExitBBs
                    }
                case _ ⇒
                    val successors = getSuccessors(curBB, MonitorExitqueuedBBs)
                    worklistMonitorExit ++= successors
                    MonitorExitqueuedBBs ++= successors
            }
        }

        val bbsEnter = {
            if (dclEnterBBs.size >= 1)
                Some(dclEnterBBs.head)
            else None
        }
        val bbsExit = {
            if (dclExitBBs.size >= 1)
                Some(dclExitBBs.head)
            else None
        }
        (result, (bbsEnter, bbsExit))

    }

    /**
     * Finds the index of the guarding if-Statement for a lazy initialization, the index of the
     * first statement executed if the field does not have its default value and the index of the
     * field read used for the guard.
     */
    def findGuard(
        fieldWrite:   Int,
        defaultValue: Any,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): (Option[(Int, Int, Int, CFGNode, Int)]) = {
        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklist: List[BasicBlock] = getPredecessors(startBB, Set.empty)

        var result: Option[(Int, Int, CFGNode, Int)] = None

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.startPC
            val endPC = curBB.endPC

            val cfStmt = code(endPC)
            (cfStmt.astID: @switch) match {
                case If.ASTID ⇒
                    val ifStmt = cfStmt.asIf
                    ifStmt.condition match {
                        case EQ if curBB != startBB && isGuard(ifStmt, defaultValue, code) ⇒
                            if (result.isDefined) {
                                if (result.get._1 != endPC || result.get._2 != endPC + 1)
                                    return None;
                            } else {
                                result = Some((endPC, endPC + 1, curBB, ifStmt.targetStmt))
                            }

                        case NE if curBB != startBB && isGuard(ifStmt, defaultValue, code) ⇒
                            if (result.isDefined) {
                                if (result.get._1 != endPC || result.get._2 != ifStmt.targetStmt)
                                    return None
                            } else {
                                result = Some((endPC, ifStmt.targetStmt, curBB, endPC + 1))
                            }

                        // Otherwise, we have to ensure that a guard is present for all predecessors
                        case _ ⇒
                            if (startPC == 0) return None;

                            val predecessors = getPredecessors(curBB, enqueuedBBs)
                            worklist ++= predecessors
                            enqueuedBBs ++= predecessors
                    }

                // Otherwise, we have to ensure that a guard is present for all predecessors
                case _ ⇒
                    if (startPC == 0) return None;

                    val predecessors = getPredecessors(curBB, enqueuedBBs)
                    worklist ++= predecessors
                    enqueuedBBs ++= predecessors
            }
        }

        if (result.isDefined) {
            // The field read that defines the value checked by the guard must be used only for the
            // guard or directly if the field's value was not the default value
            val ifStmt = code(result.get._1).asIf
            val expr = if (ifStmt.leftExpr.isConst) ifStmt.rightExpr else ifStmt.leftExpr
            val definitions = expr.asVar.definedBy
            val fieldReadUses = code(definitions.head).asAssignment.targetVar.usedBy
            val fieldReadUsedCorrectly = fieldReadUses forall { use ⇒
                use == result.get._1 || use == result.get._2
            }
            if (definitions.size == 1 && fieldReadUsedCorrectly)
                return Some((result.get._1, result.get._2, definitions.head, result.get._3, result.get._4)); // Found proper guard
        }

        None
    }

    /**
     * Checks if the field write is only executed if the field's value was still the default value.
     * Also, no exceptions may be thrown between the guarding if-Statement of a lazy initialization
     * and the field write.
     */
    def checkWriteIsGuarded(
        writeIndex:   Int,
        guardIndex:   Int,
        guardedIndex: Int,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): Boolean = {
        val startBB = cfg.bb(writeIndex).asBasicBlock
        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[BasicBlock] = List(startBB.asBasicBlock)

        val abnormalReturnNode = cfg.abnormalReturnNode

        val caughtExceptions = code filter { stmt ⇒
            stmt.astID == CaughtException.ASTID

        } flatMap { exception ⇒
            exception.asCaughtException.origins.map { origin: Int ⇒
                if (isImmediateVMException(origin)) {
                    pcOfImmediateVMException(origin)
                } else {
                    pcOfMethodExternalException(origin)
                }
            }.iterator
        }
        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.startPC
            val endPC = curBB.endPC
            if (startPC == 0 || startPC == guardedIndex)
                return false; // Reached method start or wrong branch of guarding if-Statement
            // Exception thrown between guard and write, which is ok for deterministic methods,
            // but may be a problem otherwise as the initialization is not guaranteed to happen
            // (or never happen).
            if ((curBB ne startBB) && abnormalReturnNode.predecessors.contains(curBB)) {
                if (!lazyInitializerIsDeterministic(method, code)) {
                    return false;
                }
            };
            // Exception thrown between guard and write (caught somewhere, but we don't care)
            if ((curBB ne startBB) & caughtExceptions.contains(endPC)) {
                if (!lazyInitializerIsDeterministic(method, code)) {
                    return false;
                }

            };
            // Check all predecessors except for the one that contains the guarding if-Statement
            val predecessors = getPredecessors(curBB, enqueuedBBs).filterNot(_.endPC == guardIndex)
            worklist ++= predecessors
            enqueuedBBs ++= predecessors
        }
        true
    }

    def checkWriteIsGuarded2(
        writeIndex:   Int,
        guardIndex:   Int,
        guardedIndex: Int,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): ReferenceImmutability = {
        //   println("check write is guarded 2")
        val startBB = cfg.bb(writeIndex).asBasicBlock
        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[BasicBlock] = List(startBB.asBasicBlock)
        val abnormalReturnNode = cfg.abnormalReturnNode
        val caughtExceptions = code filter { stmt ⇒
            stmt.astID == CaughtException.ASTID

        } flatMap { exception ⇒
            exception.asCaughtException.origins.map { origin: Int ⇒
                if (isImmediateVMException(origin)) {
                    pcOfImmediateVMException(origin)
                } else {
                    pcOfMethodExternalException(origin)
                }
            }.iterator
        }
        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.startPC
            val endPC = curBB.endPC
            if (startPC == 0 || startPC == guardedIndex)
                return LazyInitializedNotThreadSafeOrNotDeterministicReference; // Reached method start or wrong branch of guarding if-Statement
            // Exception thrown between guard and write, which is ok for deterministic methods,
            // but may be a problem otherwise as the initialization is not guaranteed to happen
            // (or never happen).
            if ((curBB ne startBB) && abnormalReturnNode.predecessors.contains(curBB)) {
                if (!lazyInitializerIsDeterministic(method, code)) {
                    return LazyInitializedNotThreadSafeOrNotDeterministicReference
                }
            };
            // Exception thrown between guard and write (caught somewhere, but we don't care)
            if ((curBB ne startBB) & caughtExceptions.contains(endPC)) {
                if (!lazyInitializerIsDeterministic(method, code)) {
                    return LazyInitializedNotThreadSafeOrNotDeterministicReference
                }

            };
            // Check all predecessors except for the one that contains the guarding if-Statement
            val predecessors = getPredecessors(curBB, enqueuedBBs).filterNot(_.endPC == guardIndex)
            worklist ++= predecessors
            enqueuedBBs ++= predecessors
        }
        LazyInitializedThreadSafeReference
    }

    /**
     * Gets all predecessor BasicBlocks of a CFGNode.
     */
    def getPredecessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        val result = node.predecessors.iterator flatMap { curNode ⇒
            if (curNode.isBasicBlock)
                if (visited.contains(curNode)) None
                else Some(curNode.asBasicBlock)
            else getPredecessors(curNode, visited)
        }
        result.toList
    }

    def getSuccessors(node: CFGNode, visited: Set[CFGNode]): List[BasicBlock] = {
        val result = node.successors.iterator flatMap ({
            currentNode ⇒
                if (currentNode.isBasicBlock)
                    if (visited.contains(currentNode)) None
                    else Some(currentNode.asBasicBlock)
                else getSuccessors(currentNode, visited)
        })
        result.toList
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
                if (index < 0) false
                else if (code(defSites.head).asAssignment.expr.isConst) true
                else {
                    val expr = code(index).asAssignment.expr
                    expr.isFieldRead && (expr.asFieldRead.resolveField(p) match {
                        case Some(field) ⇒
                            isImmutableReference(propertyStore(field, ReferenceImmutability.key))
                        case _ ⇒ // Unknown field
                            false
                    })
                }
            }

            defSites == SelfReferenceParameter ||
                defSites.size == 1 && isConstantDef(defSites.head)
        }

        val value = origin.expr

        val isNonConstDeterministic = value.astID match {
            case GetStatic.ASTID | GetField.ASTID ⇒
                value.asFieldRead.resolveField(p) match {
                    case Some(field) ⇒
                        isImmutableReference(propertyStore(field, ReferenceImmutability.key))
                    case _ ⇒ // Unknown field
                        false
                }
            case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                // If the value originates from a call, that call must be deterministic and may not
                // have any non constant parameters to guarantee that it is the same on every
                // invocation. The receiver object must be the 'this' self reference for the same
                // reason.
                if (value.asFunctionCall.allParams.exists(!isConstant(_))) {
                    false
                } else {
                    state.lazyInitInvocation = Some((declaredMethods(method), origin.pc))
                    true
                }
            case _ ⇒
                // The value neither is a constant nor originates from a call, but if the
                // current method does not take parameters and is deterministic, the value is
                // guaranteed to be the same on every invocation.
                lazyInitializerIsDeterministic(method, code)
        }

        value.isConst || isNonConstDeterministic
    }

    /**
     * Checks that all non-dead field reads that are not used for the guarding if-Statement of a
     * lazy initialization are only executed if the field did not have its default value or after
     * the (single) field write.
     */
    def checkReads(
        reads:        Seq[(Method, PCs)],
        readIndex:    Int,
        guardedIndex: Int,
        writeIndex:   Int,
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int]
    ): Boolean = {
        // There is only a single method with reads aside from initializers (checked by
        // isLazilyInitialized), so we have to check only reads from that one method.
        reads.filter(!_._1.isInitializer).head._2 forall { readPC ⇒
            val index = pcToIndex(readPC)
            index != -1 || index == readIndex || checkRead(index, guardedIndex, writeIndex, cfg)
        }
    }

    /**
     * Checks that a field read is only executed if the field did not have its default value or
     * after the (single) field write.
     */
    def checkRead(
        readIndex:    Int,
        guardedIndex: Int,
        writeIndex:   Int,
        cfg:          CFG[Stmt[V], TACStmts[V]]
    ): Boolean = {
        val startBB = cfg.bb(readIndex).asBasicBlock
        val writeBB = cfg.bb(writeIndex)

        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[BasicBlock] = List(startBB.asBasicBlock)

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.startPC

            if (startPC == 0)
                return false; // Reached the start of the method but not the guard or field write

            if ((curBB eq writeBB) && writeIndex > readIndex)
                return false; // In the basic block of the write, but before the write

            if (startPC != guardedIndex && // Did not reach the guard
                (curBB ne writeBB) /* Not the basic block of the write */ ) {
                val predecessors = getPredecessors(curBB, enqueuedBBs)
                worklist ++= predecessors
                enqueuedBBs ++= predecessors
            }
        }

        true
    }

    /**
     * Checks if an expression is a field read of the currently analyzed field.
     * For instance fields, the read must be on the `this` reference.
     */
    def isReadOfCurrentField(expr: Expr[V])(implicit state: State): Boolean = {
        val field = expr.astID match {
            case GetField.ASTID ⇒
                val objRefDefinition = expr.asGetField.objRef.asVar.definedBy
                if (objRefDefinition != SelfReferenceParameter) None
                else expr.asGetField.resolveField(project)
            case GetStatic.ASTID ⇒ expr.asGetStatic.resolveField(project)
            case _               ⇒ None
        }
        field.contains(state.field)
    }

    /**
     * Determines if an if-Statement is actually a guard for the current field, i.e. it compares
     * the current field to the default value.
     */
    def isGuard(
        ifStmt:       If[V],
        defaultValue: Any,
        code:         Array[Stmt[V]]
    )(implicit state: State): Boolean = {

        /**
         * Checks if an expression is an IntConst or FloatConst with the corresponding default value.
         */
        def isDefaultConst(expr: Expr[V]): Boolean = {
            if (expr.isVar) {
                val defSites = expr.asVar.definedBy
                val head = defSites.head
                defSites.size == 1 && head >= 0 && isDefaultConst(code(head).asAssignment.expr)
            } else {
                expr.isIntConst && defaultValue == expr.asIntConst.value ||
                    expr.isFloatConst && defaultValue == expr.asFloatConst.value ||
                    defaultValue == null //TODO ??
            }
        }

        /**
         * Checks whether the non-constant expression of the if-Statement is a read of the current
         * field.
         */
        def isGuardInternal(expr: V): Boolean = {
            expr.definedBy forall { index ⇒
                if (index < 0) false // If the value is from a parameter, this can not be the guard
                else isReadOfCurrentField(code(index).asAssignment.expr)
            }
        }

        if (isDefaultConst(ifStmt.leftExpr) && ifStmt.rightExpr.isVar) {
            isGuardInternal(ifStmt.rightExpr.asVar)
        } else if (isDefaultConst(ifStmt.rightExpr) && ifStmt.leftExpr.isVar) {
            isGuardInternal(ifStmt.leftExpr.asVar)
        } else false
    }

    def checkWrites(
        write:        FieldWriteAccessStmt[V],
        writeIndex:   Int,
        guardIndex:   Int,
        guardedIndex: Int,
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): Boolean = {
        val definitions = write.value.asVar.definedBy
        val isDeterministic =
            if (definitions.size == 1) {
                // The value written must be computed deterministically
                checkWriteIsDeterministic(code(definitions.head).asAssignment, method, code)
            } else {
                // More than one definition site for the value might lead to differences between
                // invocations, but not if this method has no parameters and is deterministic
                // (in this case, the definition reaching the write will always be the same)
                method.descriptor.parametersCount == 0 &&
                    !isNonDeterministic(propertyStore(declaredMethods(method), Purity.key))
            }
        val checkWriteIsGuardedResult =
            checkWriteIsGuarded(writeIndex, guardIndex, guardedIndex, method, code, cfg)
        // The field write must be guarded correctly
        isDeterministic && checkWriteIsGuardedResult

    }

    /**
     * Checks if the field write for a lazy initialization is immediately followed by a return of
     * the written value (possibly loaded by another field read).
     */
    def checkImmediateReturn(
        write:      FieldWriteAccessStmt[V],
        writeIndex: Int,
        readIndex:  Int,
        code:       Array[Stmt[V]]
    )(implicit state: State): Boolean = {
        var index = writeIndex + 1

        var load = -1

        while (index < code.length) {
            val stmt = code(index)
            stmt.astID match {
                case Assignment.ASTID ⇒
                    if (isReadOfCurrentField(stmt.asAssignment.expr))
                        load = index
                    else
                        return false; // No field read or a field read of a different field
                case ReturnValue.ASTID ⇒
                    val returnValueDefs = stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValueDefs.size == 2 &&
                        returnValueDefs.contains(write.value.asVar.definedBy.head) &&
                        returnValueDefs.contains(readIndex))
                        return true; // direct return of the written value
                    else if (load >= 0 && (returnValueDefs == IntTrieSet(load) ||
                        returnValueDefs == IntTrieSet(readIndex, load)))
                        return true; // return of field value loaded by field read
                    else
                        return false; // return of different value
                case _ ⇒ return false; // neither a field read nor a return
            }
            index += 1
        }
        false
    }

    def checkThatTheValueOfTheFieldIsReturned(
        write:      FieldWriteAccessStmt[V],
        writeIndex: Int,
        readIndex:  Int,
        code:       Array[Stmt[V]]
    )(implicit state: State): Boolean = {
        var index = writeIndex + 1

        var load = -1

        while (index < code.length) {
            val stmt = code(index)
            stmt.astID match {
                case a @ Assignment.ASTID ⇒
                    if (isReadOfCurrentField(stmt.asAssignment.expr)) {
                        load = index
                    }
                // No field read or a field read of a different field
                case ReturnValue.ASTID ⇒
                    val returnValueDefs = stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValueDefs.size == 2 &&
                        returnValueDefs.contains(write.value.asVar.definedBy.head) &&
                        returnValueDefs.contains(readIndex)) {
                        return true
                    }; // direct return of the written value
                    else if (load >= 0 && (returnValueDefs == IntTrieSet(load) ||
                        returnValueDefs == IntTrieSet(readIndex, load))) {
                        return true
                    }; // return of field value loaded by field read
                    else {
                        return false
                    }; // return of different value
                case _ ⇒ ;
            }
            index += 1
        }
        false
    }

}
