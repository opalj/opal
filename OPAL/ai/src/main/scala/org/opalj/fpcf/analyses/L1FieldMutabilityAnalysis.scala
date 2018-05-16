/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.DefinedMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.NonFinalFieldByLackOfInformation
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.LazyInitializedField
import org.opalj.fpcf.properties.FieldPrematurelyRead
import org.opalj.fpcf.properties.NotPrematurelyReadField
import org.opalj.fpcf.properties.PrematurelyReadField
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.TACode
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.FieldWriteAccessStmt
import org.opalj.tac.TACStmts
import org.opalj.tac.If
import org.opalj.RelationalOperators.EQ
import org.opalj.RelationalOperators.NE
import org.opalj.ai.pcOfVMLevelValue
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.PCs
import org.opalj.br.ComputationalTypeFloat
import org.opalj.tac.GetStatic
import org.opalj.tac.GetField
import org.opalj.tac.Assignment
import org.opalj.tac.ReturnValue
import org.opalj.tac.CaughtException

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 * @author Michael Eichberg
 */
class L1FieldMutabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    final val tacai = project.get(DefaultTACAIKey)
    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackagesKey = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val declaredMethods = project.get(DeclaredMethodsKey)
    final val isMethodOverridable = project.get(IsOverridableMethodKey)

    case class State(
            val field:                   Field,
            var fieldMutability:         FieldMutability,
            var prematurelyReadDependee: Option[EOptionP[Entity, Property]],
            var purityDependee:          Option[EOptionP[Entity, Property]]
    )

    def doDetermineFieldMutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineFieldMutability(field)
        case _ ⇒
            val m = entity.getClass.getSimpleName+" is not an org.opalj.br.Field"
            throw new IllegalArgumentException(m)
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldMutability(field: Field): PropertyComputationResult = {
        implicit val state = State(field, DeclaredFinalField, None, None)

        if (field.isFinal) {
            // Even declared final fields may not be final if they are read prematurely
            if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key)))
                return Result(field, NonFinalFieldByAnalysis);
            else
                return createResult()
        }

        state.fieldMutability = EffectivelyFinalField

        val thisType = field.classFile.thisType

        if (field.isPublic)
            return Result(field, NonFinalFieldByLackOfInformation)

        // Collect all classes that have access to the field, i.e. the declaring class and possibly
        // classes in the same package as well as subclasses
        // Give up if the set of classes having access to the field is not closed
        var classesHavingAccess: Set[ClassFile] = Set(field.classFile)

        if (field.isProtected || field.isPackagePrivate) {
            if (!closedPackagesKey.isClosed(thisType.packageName))
                return Result(field, NonFinalFieldByLackOfInformation)
            classesHavingAccess ++= project.allClassFiles.filter {
                _.thisType.packageName == thisType.packageName
            }
        }

        if (field.isProtected) {
            if (typeExtensibility(thisType).isYesOrUnknown) {
                return Result(field, NonFinalFieldByLackOfInformation)
            }
            val subTypes = classHierarchy.allSubclassTypes(thisType, reflexive = false)
            classesHavingAccess ++= subTypes.map(project.classFile(_).get)
        }

        // If there are native methods, we give up
        if (classesHavingAccess.flatMap(_.methods).exists(_.isNative))
            return Result(field, NonFinalFieldByLackOfInformation)

        // We now (compared to the simple one) have to analyze the static initializer as
        // the static initializer can be used to initialize a private field of an instance
        // of the class after the reference to the class and (an indirect) reference to the
        // field has become available. Consider the following example:
        // class X implements Y{
        //
        //     private Object o;
        //
        //     public Object getO() { return o; }
        //
        //     private static X instance;
        //     static {
        //         instance = new X();
        //         Z.register(instance);
        //         // when we reach this point o is now (via getO) publically accessible and
        //         // X is properly initialized!
        //         o = new Object(); // o is mutated...
        //     }
        // }

        val writes = fieldAccessInformation.writeAccesses(field)

        for {
            (method, pcs) ← writes
            pc ← pcs
        } {
            val TACode(_, code, pcToIndex, cfg, _, _) = tacai(method)

            val index = pcToIndex(pc)
            if (index != -1) { // Only if PutStatic/PutField is not dead
                code(index) match {
                    case stmt: PutStatic[V] ⇒
                        if (!method.isStaticInitializer) {
                            // If there is a write outside a static initializer, this might be a
                            // lazy initialization. We consider lazy initialization if there is
                            // only a single write, so we don't have to deal with synchronization
                            if (writes.size != 1 ||
                                !isLazyInitialization(stmt, index, method, code, cfg, pcToIndex))
                                return Result(field, NonFinalFieldByAnalysis);

                            state.fieldMutability = LazyInitializedField
                        }
                    case stmt: PutField[V] ⇒
                        val objRef = stmt.objRef.asVar
                        if (!method.isConstructor || objRef.definedBy != SelfReferenceParameter) {
                            // If there is a write outside a constructor, this might be a
                            // lazy initialization. We consider lazy initialization if there is
                            // only a single write, so we don't have to deal with synchronization
                            if (writes.size != 1 ||
                                !isLazyInitialization(stmt, index, method, code, cfg, pcToIndex))
                                return Result(field, NonFinalFieldByAnalysis);

                            state.fieldMutability = LazyInitializedField
                        }
                    case _ ⇒ throw new RuntimeException("unexpected field access")
                }
            }
        }

        // Prematurely read fields are not considered final
        if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key)))
            Result(field, NonFinalFieldByAnalysis)
        else
            createResult()
    }

    /**
     * Prepares the PropertyComputation result, either as IntermediateResult if there are still
     * dependees or as Result otherwise.
     */
    def createResult()(implicit state: State): PropertyComputationResult = {
        if (state.prematurelyReadDependee.isDefined || state.purityDependee.isDefined)
            IntermediateResult(
                state.field,
                NonFinalFieldByAnalysis,
                state.fieldMutability,
                state.prematurelyReadDependee ++ state.purityDependee,
                c
            )
        else Result(state.field, state.fieldMutability)
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {
        val isNotFinal = eps match {
            case fpr @ EPS(_, _, _: FieldPrematurelyRead) ⇒ isPrematurelyRead(fpr)
            case p @ EPS(_, _, _: Purity)                 ⇒ !handlePurity(eps)
        }
        if (isNotFinal) Result(state.field, NonFinalFieldByAnalysis)
        else createResult()
    }

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
        stmt:      FieldWriteAccessStmt[V],
        stmtIndex: Int,
        method:    Method,
        code:      Array[Stmt[V]],
        cfg:       CFG[Stmt[V], TACStmts[V]],
        pcToIndex: Array[Int]
    )(implicit state: State): Boolean = {
        if (state.field.fieldType.computationalType != ComputationalTypeInt &&
            state.field.fieldType.computationalType != ComputationalTypeFloat) {
            // Only handle lazy initialization of ints and floats as they are guaranteed to be
            // written atomically
            return false;
        }

        val definitions = stmt.value.asVar.definedBy

        // More than one definition site for the value might lead to differences between invocations
        if (definitions.size != 1) {
            return false;
        }

        val reads = fieldAccessInformation.readAccesses(state.field)
        if (reads.exists(_._1 ne method)) {
            return false; // Reads outside the (single) lazy initialization method
        }

        val definition = definitions.head

        // Detect only simple patterns where the lazily initialized value is returned immediately
        if (!checkImmediateReturn(stmt, stmtIndex, code))
            return false;

        val owner = if (state.field.isStatic) null else stmt.asPutField.objRef

        // There must be a guarding if-Statement
        // The guardIndex is the index of the if-Statement, the guardedIndex is the index of the
        // first statement that is executed after the if-Statement if the field's value was not the
        // default value
        val (guardIndex, guardedIndex) = findGuard(stmtIndex, code, cfg, owner) match {
            case Some((guardIndex, guardedIndex)) ⇒ (guardIndex, guardedIndex)
            case None                             ⇒ return false;
        }

        // The value written must be computed deterministically
        if (!checkWriteIsDeterministic(code(definition).asAssignment.expr, method, code))
            return false;

        // The field write must be guarded correctly
        if (!checkWriteIsGuarded(definition, guardIndex, guardedIndex, code, cfg))
            return false;

        // The field read used for the guard may be used only for the guard or at the guardedIndex
        if (!checkGuard(guardIndex, guardedIndex, code))
            return false;

        // Field reads (except for the guard) may only be executed if the field's value is not the
        // default value
        if (!checkReads(reads, guardIndex, guardedIndex, stmtIndex, cfg, pcToIndex))
            return false;

        true
    }

    /**
     * Checks if the field write for a lazy initialization is immediately followed by a return of
     * the written value (possibly loaded by another field read).
     */
    def checkImmediateReturn(
        write:      FieldWriteAccessStmt[V],
        writeIndex: Int,
        code:       Array[Stmt[V]]
    )(implicit state: State): Boolean = {
        var index = writeIndex + 1

        var foundReturn = false
        var load: Option[Int] = None

        while (index < code.length && !foundReturn) {
            val stmt = code(index)
            stmt.astID match {
                case Assignment.ASTID ⇒
                    val expr = stmt.asAssignment.expr
                    val field = expr.astID match {
                        case GetField.ASTID if (expr.asGetField.objRef.asVar.definedBy == write.asPutField.objRef.asVar.definedBy) ⇒
                            expr.asGetField.resolveField(project)
                        case GetStatic.ASTID ⇒ expr.asGetStatic.resolveField(project)
                        case _               ⇒ None
                    }
                    if (field.exists(_ == state.field))
                        load = Some(index)
                    else
                        return false; // No field read or a field read of a different field
                case ReturnValue.ASTID ⇒
                    val returnValue = stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValue.contains(write.value.asVar.definedBy.head))
                        foundReturn = true // direct return of the written value
                    else if (load.isDefined && returnValue.contains(load.get))
                        foundReturn = true // return of field value loaded by field read
                    else
                        return false; // return of different value
                case _ ⇒ return false; // neither a field read nor a return
            }
            index += 1
        }
        true
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
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    ): Boolean = {
        val startBB = cfg.bb(writeIndex).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[CFGNode] = List(startBB)

        val abnormalReturnNode = cfg.abnormalReturnNode
        val caughtExceptions = code filter { stmt ⇒
            stmt.astID == CaughtException.ASTID
        } flatMap (_.asCaughtException.origins.map(pcOfVMLevelValue _).iterator)

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.asBasicBlock.startPC
            if (startPC == 0 || startPC == guardedIndex)
                return false; // Reached method start or wrong branch of guarding if-Statement

            if ((curBB ne startBB) && abnormalReturnNode.predecessors.contains(curBB))
                return false; // Exception thrown between guard and write

            if ((curBB ne startBB) & caughtExceptions.exists(_ == curBB.asBasicBlock.endPC)) {
                // Exception thrown between guard and write (caught somewhere, but we don't care)
                return false;
            }

            // Check all predecessors except for the one that contains the guarding if-Statement
            val predecessors = curBB.predecessors filterNot { bb ⇒
                enqueuedBBs.contains(bb) || bb.asBasicBlock.endPC == guardIndex
            }
            worklist ++= predecessors
            enqueuedBBs ++= predecessors
        }

        true
    }

    /**
     * Checks if the value written to the field is guaranteed to be always the same.
     * This is true if the value is constant or originates from a deterministic call of a method
     * without non-constant parameters. Alternatively, if the initialization method itself is
     * deterministic and has no parameters, the value is also always the same.
     */
    def checkWriteIsDeterministic(
        value:  Expr[V],
        method: Method,
        code:   Array[Stmt[V]]
    )(implicit state: State): Boolean = {
        val isNonConstDeterministic = value.astID match {
            // If the value originates from a call, that call must be deterministic and may not have
            // any non constant parameters to guarantee that it is the same on every invocation.
            // The receiver object must be the 'this' self reference for the same reason.
            case StaticFunctionCall.ASTID ⇒
                val callee = value.asStaticFunctionCall.resolveCallTarget(p)
                val params = value.asStaticFunctionCall.params
                handleCall(callee, code, params)
            case NonVirtualFunctionCall.ASTID if value.asNonVirtualFunctionCall.receiver.asVar.definedBy == SelfReferenceParameter ⇒
                val callee = value.asNonVirtualFunctionCall.resolveCallTarget(p)
                val NonVirtualFunctionCall(_, _, _, _, _, receiver, params) = value
                handleCall(callee, code, params)
            case VirtualFunctionCall.ASTID if value.asVirtualFunctionCall.receiver.asVar.definedBy == SelfReferenceParameter ⇒
                val VirtualFunctionCall(_, declClass, _, name, descr, rcvr, params) = value
                val thisType = method.classFile.thisType
                handleVirtualCall(code, thisType, declClass, name, rcvr, params, descr)
            case _ if (method.descriptor.parametersCount == 0) ⇒
                // The value neither is a constant nor originates from a call, but as the
                // current method does not take parameters, the value is guaranteed to be the
                // same on every invocation if the current method is deterministic.
                handleCall(Success(method), code, Seq.empty)
            case _ ⇒ false
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
        guardIndex:   Int,
        guardedIndex: Int,
        writeIndex:   Int,
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int]
    ): Boolean = {
        reads.head._2 forall { readPC ⇒
            val index = pcToIndex(readPC)
            (index != -1 && index != guardIndex) || checkRead(index, guardedIndex, writeIndex, cfg)
        }
    }

    /**
     * Checks that the value used for the guarding if-Statement of a lazy initializaiton is used
     * only for that statement or immediately if the field's value was not the default value.
     */
    def checkGuard(guardIndex: Int, guardedIndex: Int, code: Array[Stmt[V]]): Boolean = {
        val guard = code(guardIndex).asIf
        val expr = if (isDefaultConst(guard.leftExpr, code)) guard.rightExpr else guard.leftExpr
        expr.asVar.definedBy forall { definition ⇒
            val fieldReadUses = code(definition).asAssignment.targetVar.usedBy
            fieldReadUses forall { use ⇒
                use == guardIndex || use == guardedIndex
            }
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

        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[CFGNode] = List(startBB)

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val startPC = curBB.asBasicBlock.startPC
            val endPC = curBB.asBasicBlock.endPC

            if (startPC == 0)
                return false; // Reached the start of the method but not the guard or field write

            if (endPC >= readIndex && startPC < readIndex && writeIndex > readIndex)
                return false; // In the basic block of the write, but before the write

            if (startPC != guardedIndex && // Did not reach the guard
                (endPC <= writeIndex || startPC > writeIndex) // Not the basic block of the write
                ) {
                val predecessors = curBB.predecessors.filterNot(enqueuedBBs.contains)
                worklist ++= predecessors
                enqueuedBBs ++= predecessors
            }
        }

        true
    }

    /**
     * Finds the index of the guarding if-Statement for a lazy initialization and the index of the
     * first statement executed if the field does not have its default value.
     */
    def findGuard(
        fieldWrite: Int,
        code:       Array[Stmt[V]],
        cfg:        CFG[Stmt[V], TACStmts[V]],
        owner:      Expr[V]
    )(implicit state: State): Option[(Int, Int)] = {
        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = Set(startBB)
        var worklist: List[CFGNode] = List(startBB)

        while (worklist.nonEmpty) {
            val curBB = worklist.head
            worklist = worklist.tail

            val endPC = curBB.asBasicBlock.endPC
            val cfStmt = code(endPC)
            cfStmt.astID match {
                case If.ASTID ⇒
                    val ifStmt = cfStmt.asIf
                    ifStmt.condition match {
                        case EQ if curBB != startBB && isGuard(ifStmt, code, owner) ⇒
                            // Tracing back from the field write in a breadth first search lead to
                            // an empty worklist if the field write is control-dependent on the
                            // guard (as required) and to a non-empty worklist otherwise
                            return if (worklist.isEmpty) Some((endPC, endPC + 1)) else None;

                        case NE if curBB != startBB && isGuard(ifStmt, code, owner) ⇒
                            return if (worklist.isEmpty) Some((endPC, ifStmt.targetStmt)) else None;

                        // Otherwise, we have to ensure that a guard is present for all predecessors
                        case _ ⇒
                            val start = curBB.asBasicBlock.startPC
                            if (start == 0) return None;

                            val predecessors = curBB.predecessors.filterNot(enqueuedBBs.contains)
                            worklist ++= predecessors
                            enqueuedBBs ++= predecessors
                    }

                // Otherwise, we have to ensure that a guard is present for all predecessors
                case _ ⇒
                    val start = curBB.asBasicBlock.startPC
                    if (start == 0) return None;

                    val predecessors = curBB.predecessors.filterNot(enqueuedBBs.contains)
                    worklist ++= predecessors
                    enqueuedBBs ++= predecessors
            }
        }

        None
    }

    /**
     * Determines if an if-Statement is actually a guard for the current field, i.e. it compares
     * the current field to the default value.
     */
    def isGuard(
        ifStmt: If[V],
        code:   Array[Stmt[V]],
        owner:  Expr[V]
    )(implicit state: State): Boolean = {

        /**
         * Checks whether the non-constant expression of the if-Statement is a read of the current
         * field.
         */
        def isGuardInternal(expr: V): Boolean = {
            expr.definedBy forall { index ⇒
                val definition = code(index).asAssignment.expr
                val field = definition.astID match {
                    // For instance fields, the read must be on the same object as the field write
                    case GetField.ASTID if (definition.asGetField.objRef.asVar.definedBy == owner.asVar.definedBy) ⇒
                        definition.asGetField.resolveField(project)
                    case GetStatic.ASTID ⇒ definition.asGetStatic.resolveField(project)
                    case _               ⇒ None
                }
                field.exists(_ == state.field)
            }
        }

        if (isDefaultConst(ifStmt.leftExpr, code)) {
            isGuardInternal(ifStmt.rightExpr.asVar)
        } else if (isDefaultConst(ifStmt.rightExpr, code)) {
            isGuardInternal(ifStmt.leftExpr.asVar)
        } else false
    }

    /**
     * Checks if an expression is an IntConst or FloatConst with the corresponding default value.
     */
    def isDefaultConst(expr: Expr[V], code: Array[Stmt[V]]): Boolean = {
        expr.isIntConst && expr.asIntConst.value == 0 ||
            expr.isFloatConst && expr.asFloatConst.value == 0.0f
    }

    /**
     * Checks if the field is prematurely read, i.e. read before it is initialized in the
     * constructor, using the corresponding property.
     */
    def isPrematurelyRead(eop: EOptionP[Entity, Property])(implicit state: State): Boolean =
        eop match {
            case EPS(_, NotPrematurelyReadField, _) ⇒
                state.prematurelyReadDependee = None
                false
            case EPS(_, _, PrematurelyReadField) ⇒ true
            case eps ⇒
                state.prematurelyReadDependee = Some(eps)
                false
        }

    /**
     * Checkes if the method that defines the value assigned to a (potentially) lazily initialized
     * field is deterministic, ensuring that the same value is written even for concurrent
     * executions.
     */
    def handlePurity(eop: EOptionP[Entity, Property])(implicit state: State): Boolean = eop match {
        case EPS(_, p: Purity, _) if p.isDeterministic ⇒
            state.purityDependee = None
            true
        case EPS(_, VirtualMethodPurity(p), _) if p.isDeterministic ⇒
            state.purityDependee = None
            true
        case EPS(_, _, p: Purity) if !p.isDeterministic              ⇒ false
        case EPS(_, _, VirtualMethodPurity(p)) if !p.isDeterministic ⇒ false
        case _ ⇒
            state.purityDependee = Some(eop)
            true
    }

    /**
     * Checks if the call to a method that defines the value assigned to a (potentially) lazily
     * initialized field is deterministic ensuring that the same value is written even for
     * concurrent executions.
     * This requires a deterministic method and constant parameters.
     */
    def handleCall(
        target:     opalj.Result[Method],
        code:       Array[Stmt[V]],
        parameters: Seq[Expr[V]]
    )(implicit state: State): Boolean = target match {
        case Success(callee) ⇒
            if (!parameters.forall(_.asVar.definedBy.forall(code(_).asAssignment.expr.isConst)))
                false
            else handlePurity(propertyStore(declaredMethods(callee), Purity.key))
        case _ ⇒ false // Field is initialized by unknown method
    }

    /**
     * Resolves the potential call targets for a virtual call and then checks whether they are
     * deterministic. Also, all parameters must be constant.
     */
    def handleVirtualCall(
        code:      Array[Stmt[V]],
        declClass: ObjectType,
        rcvrType:  ReferenceType,
        name:      String,
        receiver:  Expr[V],
        params:    Seq[Expr[V]],
        descr:     MethodDescriptor
    )(implicit state: State): Boolean = {
        if (receiver.asVar.value.asDomainReferenceValue.isNull.isYes)
            // IMPROVE Just use the CFG to check if we have a normal successor
            return true; // We don't have to examine calls that will result in an NPE

        val rcvrTypeBound =
            if (receiver.isVar) {
                project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                    receiver.asVar.value.asDomainReferenceValue.upperTypeBound
                )
            } else {
                rcvrType
            }
        val receiverType =
            if (rcvrTypeBound.isArrayType) ObjectType.Object
            else rcvrTypeBound.asObjectType

        //TODO Is this correct now?
        val callee = project.instanceCall(declClass, receiverType, name, descr)
        val calleeR =
            if (callee.hasValue) callee
            else project.classFile(receiverType) match {
                case Some(cf) if cf.isInterfaceDeclaration ⇒
                    org.opalj.Result(
                        project.resolveInterfaceMethodReference(receiverType, name, descr)
                    )
                case Some(_) ⇒ project.resolveClassMethodReference(receiverType, name, descr)
                case _       ⇒ Failure
            }

        if (receiver.isVar && receiver.asVar.value.asDomainReferenceValue.isPrecise ||
            rcvrTypeBound.isArrayType) {
            // The receiver could refer to further expressions in a non-flat representation.
            // To avoid special handling, we just fallback to the general case of virtual/interface
            // calls here as the analysis is intended to be used on flat representations anyway.
            handleCall(calleeR, code, params)
        } else {
            /*val cfo = if (rcvrTypeBound.isArrayType) project.ObjectClassFile
            else project.classFile(rcvrTypeBound.asObjectType)
            val methodO = cfo.flatMap(_.findMethod(name, descr))*/
            if (calleeR.isEmpty || isMethodOverridable(calleeR.value).isNotNo) {
                false // We don't know all overrides
            } else {
                // Get the DefinedMethod for this call site
                val dm = declaredMethods(DefinedMethod(receiverType, calleeR.value))

                if (!params.forall(_.asVar.definedBy.forall(code(_).asAssignment.expr.isConst)))
                    false
                else handlePurity(propertyStore(dm, VirtualMethodPurity.key))
            }
        }
    }
}

trait L1FieldMutabilityAnalysisScheduler extends ComputationSpecification {
    override def uses: Set[PropertyKind] = Set(Purity)

    override def derives: Set[PropertyKind] = Set(FieldMutability)
}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldMutabilityAnalysis extends L1FieldMutabilityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(project)

        val fields = project.allFields

        propertyStore.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldMutability)
        analysis
    }
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL1FieldMutabilityAnalysis extends L1FieldMutabilityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldMutability.key, analysis.determineFieldMutability
        )
        analysis
    }
}
