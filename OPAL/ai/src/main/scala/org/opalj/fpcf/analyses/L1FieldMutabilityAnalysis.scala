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
import org.opalj.br.FloatType
import org.opalj.br.cfg.BasicBlock
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.NonFinalField
import org.opalj.tac.GetStatic
import org.opalj.tac.GetField
import org.opalj.tac.Assignment
import org.opalj.tac.ReturnValue
import org.opalj.tac.CaughtException
import org.opalj.tac.InstanceFunctionCall
import org.opalj.tac.NonVirtualMethodCall

import scala.annotation.switch

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kübler
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
            field:                       Field,
            var fieldMutability:         FieldMutability,
            var prematurelyReadDependee: Option[EOptionP[Entity, Property]],
            var purityDependee:          Option[EOptionP[Entity, Property]],
            var fieldMutabilityDependee: Option[EOptionP[Entity, Property]]
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
        implicit val state: State = State(field, DeclaredFinalField, None, None, None)

        // Fields are not final if they are read prematurely!
        if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key)))
            return Result(field, NonFinalFieldByAnalysis);

        if (field.isFinal)
            return createResult();

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

        val methodsHavingAccess = classesHavingAccess.flatMap(_.methods);

        // If there are native methods, we give up
        if (methodsHavingAccess.exists(_.isNative))
            return Result(field, NonFinalFieldByLackOfInformation);

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

        val default = getDefaultValue(methodsHavingAccess)

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            pc ← pcs
        } {

            val TACode(_, code, pcToIndex, cfg, _, _) = tacai(method)

            val index = pcToIndex(pc)
            if (index != -1) { // Only if PutStatic/PutField is not dead
                val stmt = code(index)
                stmt.astID match {
                    case PutStatic.ASTID | PutField.ASTID ⇒
                        if (method.isInitializer) {
                            if (field.isStatic) {
                                if (method.isConstructor)
                                    return Result(field, NonFinalFieldByAnalysis);
                            } else {
                                val receiverDefs = stmt.asPutField.objRef.asVar.definedBy
                                if (receiverDefs != SelfReferenceParameter)
                                    return Result(field, NonFinalFieldByAnalysis);
                            }
                        } else {
                            // If different default values are assigned => not a lazy initialization
                            if (default.isEmpty)
                                return Result(field, NonFinalFieldByAnalysis);

                            // We consider lazy initialization if there is only single write outside
                            // an initializer, so we can ignore synchronization
                            if (state.fieldMutability == LazyInitializedField)
                                return Result(field, NonFinalFieldByAnalysis);

                            // A lazily initialized instance field must be initialized only by its
                            // owning instance
                            if (!field.isStatic &&
                                stmt.asPutField.objRef.asVar.definedBy != SelfReferenceParameter)
                                return Result(field, NonFinalFieldByAnalysis);

                            // A field written outside an initializer must be lazily initialized or
                            // it is non-final
                            if (!isLazyInitialization(index, default, method, code, cfg, pcToIndex))
                                return Result(field, NonFinalFieldByAnalysis);

                            state.fieldMutability = LazyInitializedField
                        }

                    case _ ⇒ throw new RuntimeException("unexpected field access")
                }
            }
        }

        createResult()
    }

    /**
     * Returns the value the field will have after initialization or None if there may be multiple
     * values.
     */
    def getDefaultValue(methodsHavingAccess: Set[Method])(implicit state: State): Option[Any] = {
        var constantVal: Option[Any] = None
        var allInitializeConstant = true

        val methodsIterator = methodsHavingAccess.iterator
        while (methodsIterator.hasNext && allInitializeConstant) {
            val method = methodsIterator.next()
            if (method.isInitializer) {
                val code = tacai(method).stmts
                val maxIndex = code.length
                var index = 0
                var foundInit = false
                while (index < maxIndex) {
                    val stmt = code(index)
                    (stmt.astID: @switch) match {
                        case PutStatic.ASTID | PutField.ASTID ⇒
                            if (stmt.astID == PutStatic.ASTID ||
                                stmt.asPutField.objRef.asVar.definedBy == SelfReferenceParameter) {
                                val write = stmt.asFieldWriteAccessStmt
                                if (write.resolveField(p).contains(state.field)) {
                                    val defs = write.value.asVar.definedBy
                                    if (defs.size == 1 && defs.head >= 0) {
                                        val defSite = code(defs.head).asAssignment.expr
                                        val const = if (defSite.isIntConst)
                                            Some(defSite.asIntConst.value)
                                        else if (defSite.isFloatConst)
                                            Some(defSite.asFloatConst.value)
                                        else None
                                        if (const.isDefined) {
                                            foundInit = true
                                            if (constantVal.isDefined) {
                                                if (constantVal != const) {
                                                    allInitializeConstant = false
                                                    constantVal = None
                                                }
                                            } else constantVal = const
                                        }
                                    }
                                }
                            }

                        case NonVirtualMethodCall.ASTID ⇒
                            val NonVirtualMethodCall(_, declClass, _, name, _, rcvr, _) = stmt
                            // Consider calls to other constructors as initializations as either
                            // the called constructor will initialize the field or delegate to yet
                            // another constructor
                            if (declClass == state.field.classFile.thisType && name == "<init>" &&
                                rcvr.asVar.definedBy == SelfReferenceParameter)
                                foundInit = true

                        case _ ⇒
                    }
                    index += 1
                }
                if (!foundInit) {
                    if (constantVal.isDefined) allInitializeConstant = false
                    else constantVal = Some(if (state.field.fieldType eq FloatType) 0.0f else 0)
                }
            }
        }

        constantVal
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
                state.prematurelyReadDependee ++
                    state.purityDependee ++
                    state.fieldMutabilityDependee,
                c
            )
        else
            Result(state.field, state.fieldMutability)
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {
        val isNotFinal = eps match {
            case EPS(_, _, _: FieldPrematurelyRead) ⇒ isPrematurelyRead(eps)
            case EPS(_, _, _: Purity)               ⇒ !isDeterministic(eps)
            case EPS(_, _, _: FieldMutability)      ⇒ !isFinalField(eps)
        }

        if (isNotFinal)
            Result(state.field, NonFinalFieldByAnalysis)
        else
            createResult()
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
        writeIndex:   Int,
        defaultValue: Option[Any],
        method:       Method,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]],
        pcToIndex:    Array[Int]
    )(implicit state: State): Boolean = {
        val write = code(writeIndex).asFieldWriteAccessStmt

        if (state.field.fieldType.computationalType != ComputationalTypeInt &&
            state.field.fieldType.computationalType != ComputationalTypeFloat) {
            // Only handle lazy initialization of ints and floats as they are guaranteed to be
            // written atomically
            return false;
        }

        val reads = fieldAccessInformation.readAccesses(state.field)
        if (reads.exists(mAndPCs ⇒ (mAndPCs._1 ne method) && !mAndPCs._1.isInitializer)) {
            return false; // Reads outside the (single) lazy initialization method
        }

        // There must be a guarding if-Statement
        // The guardIndex is the index of the if-Statement, the guardedIndex is the index of the
        // first statement that is executed after the if-Statement if the field's value was not the
        // default value
        val (guardIndex, guardedIndex, readIndex) =
            findGuard(writeIndex, defaultValue, code, cfg) match {
                case Some((guard, guarded, read)) ⇒ (guard, guarded, read)
                case None                         ⇒ return false;
            }

        // Detect only simple patterns where the lazily initialized value is returned immediately
        if (!checkImmediateReturn(write, writeIndex, readIndex, code))
            return false;

        // The value written must be computed deterministically and the writes guarded correctly
        if (!checkWrites(write, writeIndex, guardIndex, guardedIndex, method, code, cfg))
            return false;

        // Field reads (except for the guard) may only be executed if the field's value is not the
        // default value
        if (!checkReads(reads, readIndex, guardedIndex, writeIndex, cfg, pcToIndex))
            return false;

        true
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
                checkWriteIsDeterministic(code(definitions.head).asAssignment.expr, method, code)
            } else {
                // More than one definition site for the value might lead to differences between
                // invocations, but not if this method has no parameters and is deterministic
                // (in this case, the definition reaching the write will always be the same)
                method.descriptor.parametersCount == 0 &&
                    handleCall(Success(method), code, Seq.empty)
            }

        // The field write must be guarded correctly
        isDeterministic &&
            checkWriteIsGuarded(writeIndex, guardIndex, guardedIndex, method, code, cfg)
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

    def lazyInitializerIsDeterministic(
        method: Method,
        code:   Array[Stmt[V]]
    )(implicit state: State): Boolean = {
        method.descriptor.parametersCount == 0 && handleCall(Success(method), code, Seq.empty)
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
        } flatMap (_.asCaughtException.origins.map(pcOfVMLevelValue _).iterator)

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
            if ((curBB ne startBB) && abnormalReturnNode.predecessors.contains(curBB))
                if (!lazyInitializerIsDeterministic(method, code))
                    return false;

            // Exception thrown between guard and write (caught somewhere, but we don't care)
            if ((curBB ne startBB) & caughtExceptions.contains(endPC))
                if (!lazyInitializerIsDeterministic(method, code))
                    return false;

            // Check all predecessors except for the one that contains the guarding if-Statement
            val predecessors = getPredecessors(curBB, enqueuedBBs).filterNot(_.endPC == guardIndex)
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
        def receiverIsConstant(call: InstanceFunctionCall[V]): Boolean = {
            val defSites = call.receiver.asVar.definedBy

            def isDefFromFinalField(index: Int) = {
                if (index < 0) false
                else {
                    val expr = code(index).asAssignment.expr
                    expr.isFieldRead && (expr.asFieldRead.resolveField(p) match {
                        case Some(field) ⇒
                            isFinalField(propertyStore(field, FieldMutability.key))
                        case _ ⇒ // Unknown field
                            false
                    })
                }
            }

            defSites == SelfReferenceParameter ||
                defSites.size == 1 && isDefFromFinalField(defSites.head)
        }

        val isNonConstDeterministic = value.astID match {
            // If the value originates from a call, that call must be deterministic and may not have
            // any non constant parameters to guarantee that it is the same on every invocation.
            // The receiver object must be the 'this' self reference for the same reason.
            case GetStatic.ASTID | GetField.ASTID ⇒
                value.asFieldRead.resolveField(p) match {
                    case Some(field) ⇒
                        isFinalField(propertyStore(field, FieldMutability.key))
                    case _ ⇒ // Unknown field
                        false
                }
            case StaticFunctionCall.ASTID ⇒
                val callee = value.asStaticFunctionCall.resolveCallTarget(p)
                val params = value.asStaticFunctionCall.params
                handleCall(callee, code, params)
            case NonVirtualFunctionCall.ASTID if receiverIsConstant(value.asInstanceFunctionCall) ⇒
                val callee = value.asNonVirtualFunctionCall.resolveCallTarget(p)
                val NonVirtualFunctionCall(_, _, _, _, _, _, params) = value
                handleCall(callee, code, params)
            case VirtualFunctionCall.ASTID if receiverIsConstant(value.asInstanceFunctionCall) ⇒
                val VirtualFunctionCall(_, declClass, _, name, descr, rcvr, params) = value
                val thisType = method.classFile.thisType
                handleVirtualCall(code, thisType, declClass, name, rcvr, params, descr)
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

    /**
     * Finds the index of the guarding if-Statement for a lazy initialization, the index of the
     * first statement executed if the field does not have its default value and the index of the
     * field read used for the guard.
     */
    def findGuard(
        fieldWrite:   Int,
        defaultValue: Option[Any],
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    )(implicit state: State): Option[(Int, Int, Int)] = {
        val startBB = cfg.bb(fieldWrite).asBasicBlock

        var enqueuedBBs: Set[CFGNode] = startBB.predecessors
        var worklist: List[BasicBlock] = getPredecessors(startBB, Set.empty)

        var result: Option[(Int, Int)] = None

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
                                result = Some((endPC, endPC + 1))
                            }

                        case NE if curBB != startBB && isGuard(ifStmt, defaultValue, code) ⇒
                            if (result.isDefined) {
                                if (result.get._1 != endPC || result.get._2 != ifStmt.targetStmt)
                                    return None;
                            } else {
                                result = Some((endPC, ifStmt.targetStmt))
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
                return Some((result.get._1, result.get._2, definitions.head)); // Found proper guard
        }

        None
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
        defaultValue: Option[Any],
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
                expr.isIntConst && defaultValue.contains(expr.asIntConst.value) ||
                    expr.isFloatConst && defaultValue.contains(expr.asFloatConst.value)
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
    def isDeterministic(eop: EOptionP[Entity, Property])(implicit state: State): Boolean = eop match {
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
     * Checkes if the method that defines the value assigned to a (potentially) lazily initialized
     * field is deterministic, ensuring that the same value is written even for concurrent
     * executions.
     */
    def isFinalField(eop: EOptionP[Entity, Property])(implicit state: State): Boolean = eop match {
        case EPS(_, _: FinalField, _) ⇒
            state.fieldMutabilityDependee = None
            true
        case EPS(_, _, _: NonFinalField) ⇒ false
        case _ ⇒
            state.fieldMutabilityDependee = Some(eop)
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
            if (parameters.exists(_.asVar.definedBy exists { defSite ⇒
                defSite < 0 || !code(defSite).asAssignment.expr.isConst
            }))
                false
            else isDeterministic(propertyStore(declaredMethods(callee), Purity.key))
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

        val typeBound =
            project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                receiver.asVar.value.asDomainReferenceValue.upperTypeBound
            )

        if (receiver.asVar.value.asDomainReferenceValue.isNull.isYes) {
            true // We don't have to examine calls that will result in an NPE
        } else if (typeBound.isArrayType) {
            val callee = project.instanceCall(declClass, ObjectType.Object, name, descr)
            handleCall(callee, code, params)
        } else if (receiver.asVar.value.asDomainReferenceValue.isPrecise) {
            val preciseType = receiver.asVar.value.asDomainReferenceValue.valueType.get
            val callee = project.instanceCall(declClass, preciseType, name, descr)
            handleCall(callee, code, params)
        } else {
            val callee =
                declaredMethods(declClass.packageName, typeBound.asObjectType, name, descr)

            if (!callee.hasDefinition || isMethodOverridable(callee.methodDefinition).isNotNo) {
                false // We don't know all overrides
            } else {
                if (params.exists(_.asVar.definedBy exists { defSite ⇒
                    defSite < 0 || !code(defSite).asAssignment.expr.isConst
                }))
                    false
                else isDeterministic(propertyStore(callee, VirtualMethodPurity.key))
            }
        }
    }
}

trait L1FieldMutabilityAnalysisScheduler extends ComputationSpecification {
    override def uses: Set[PropertyKind] = Set(Purity, FieldPrematurelyRead)
    override def derives: Set[PropertyKind] = Set(FieldMutability)
}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

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
object LazyL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldMutability.key, analysis.determineFieldMutability
        )
        analysis
    }
}
