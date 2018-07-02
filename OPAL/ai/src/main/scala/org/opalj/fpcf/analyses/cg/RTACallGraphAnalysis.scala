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
package cg

import java.util.concurrent.atomic.AtomicBoolean

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.properties.AllTypes
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CalleesImplementation
import org.opalj.fpcf.properties.Callers
import org.opalj.fpcf.properties.CallersImplementation
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.Invokedynamic
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall

import scala.collection.Set
import scala.collection.immutable.IntMap
import scala.collection.mutable.ArrayBuffer

case class State(
        private[cg] val method:            DefinedMethod,
        private[cg] val virtualCallSites:  Traversable[(Int /*PC*/ , ObjectType, String, MethodDescriptor)],
        private var _calleesOfM:           IntMap[IntTrieSet], // key = PC
        private[cg] var numTypesProcessed: Int
) {
    private[cg] def addCallEdge(pc: Int, tgtId: Int): Unit = {
        _calleesOfM = calleesOfM.updated(pc, calleesOfM.getOrElse(pc, IntTrieSet.empty) + tgtId)
    }

    private[cg] def calleesOfM: IntMap[IntTrieSet] = _calleesOfM
}

/**
 * An rapid type call graph analysis (RTA). For a given [[Method]] it computes the set of outgoing
 * call edges ([[Callees]]). Furthermore, it updates the types for which allocations are present in
 * the [[SomeProject]] ([[InstantiatedTypes]]) and updates the [[org.opalj.fpcf.properties.Callers]].
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * For each MethodId ([[org.opalj.br.analyses.DeclaredMethods]]), it states whether the method
     * has been analysed using `step1` or not.
     */
    val processedMethods: Array[AtomicBoolean] = {
        Array.fill(declaredMethods.size) { new AtomicBoolean } //TODO this does not work with dms
    }

    def step1(
        declaredMethod: DeclaredMethod
    ): PropertyComputationResult = {

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType ne declaredMethod.declaringClassType)
            return NoResult;

        // we must only call this method once per method.
        val methodID = declaredMethods.methodID(declaredMethod)
        assert(processedMethods(methodID).get())

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // the set of methods that become reachable due to the current method and instantiated types
        val newReachableMethods = ArrayBuffer.empty[Method]

        // the set of types that are definitely initialized at this point in time
        // in case the instantiatedTypes are not finally computed, we depend on them
        val instantiatedTypesEOptP: SomeEOptionP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case EPS(_, _, ub: InstantiatedTypes) ⇒
                ub.types

            case _ ⇒
                InstantiatedTypes.initialTypes
        }

        // process each stmt in the current method to compute:
        //  1. newly allocated types
        //  2. methods (+ pc) called by the current method
        //  3. compute the call sites of virtual calls, whose targets are not yet final
        //  4. add newly reachable methods
        val (newInstantiatedTypes, calleesOfM, virtualCallSites) = handleStmts(
            method, instantiatedTypesUB, newReachableMethods
        )

        // the number of types, already seen by the analysis
        val numTypesProcessed = instantiatedTypesUB.size

        val state = State(
            declaredMethod.asDefinedMethod, virtualCallSites, calleesOfM, numTypesProcessed
        )

        // here we can ignore the return value, as the state also gets updated
        handleVirtualCallSites(state, instantiatedTypesUB.iterator, newReachableMethods, calleesOfM)

        var results = Seq(resultForCallees(instantiatedTypesEOptP, state))
        if (newInstantiatedTypes.nonEmpty)
            results +:= partialResultForInstantiatedTypes(method, newInstantiatedTypes)

        if (state.calleesOfM.nonEmpty)
            results ++:= partialResultForCallers(declaredMethod.asDefinedMethod, state.calleesOfM)

        IncrementalResult(
            Results(results),
            // continue the computation with the newly reachable methods
            newReachableMethods.map(nextMethod ⇒ (step1 _, declaredMethods(nextMethod)))
        )
    }

    def continuation(
        instantiatedTypesEOptP: SomeEPS,
        state:                  State
    ): PropertyComputationResult = {
        // find the new types, that should be processed
        val newInstantiatedTypes = instantiatedTypesEOptP match {
            case EPS(_, _, ub: InstantiatedTypes) ⇒
                val toBeDropped = state.numTypesProcessed
                state.numTypesProcessed = ub.numElements
                ub.getNewTypes(toBeDropped)
            case _ ⇒ Iterator.empty // the initial types are already processed
        }

        // the methods that become reachable due to the new types
        val newReachableMethods = ArrayBuffer.empty[Method]

        // the new edges in the call graph due to the new types
        val newCalleesOfM = handleVirtualCallSites(
            state, newInstantiatedTypes, newReachableMethods, IntMap.empty
        )

        var results = Seq(resultForCallees(instantiatedTypesEOptP, state))
        if (newCalleesOfM.nonEmpty)
            results ++:= partialResultForCallers(state.method, state.calleesOfM)

        IncrementalResult(
            Results(results),
            newReachableMethods.map(nextMethod ⇒ (step1 _, declaredMethods(nextMethod)))
        )
    }

    def handleStmts(
        method:              Method,
        instantiatedTypesUB: UIDSet[ObjectType],
        newReachableMethods: ArrayBuffer[Method]
    ): (UIDSet[ObjectType], IntMap[IntTrieSet], Traversable[(Int, ObjectType, String, MethodDescriptor)]) = {
        implicit val p: SomeProject = project

        // for each call site in the current method, the set of methods that might called
        var calleesOfM = IntMap.empty[IntTrieSet]

        // the virtual call sites, where we can not determine the precise tgts
        var virtualCallSites = List.empty[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = UIDSet.empty[ObjectType]

        val stmts = tacaiProvider(method).stmts

        // for allocation sites, add new types
        // for calls, add new edges
        for (stmt ← stmts) {
            stmt match {
                case Assignment(_, _, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                    }

                case ExprStmt(_, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                    }

                case StaticFunctionCallStatement(call) ⇒
                    calleesOfM = handleCall(
                        stmt.pc, call.resolveCallTarget.toSet, newReachableMethods, calleesOfM
                    )

                case call: StaticMethodCall[V] ⇒
                    calleesOfM = handleCall(
                        stmt.pc, call.resolveCallTarget.toSet, newReachableMethods, calleesOfM
                    )

                case NonVirtualFunctionCallStatement(call) ⇒
                    calleesOfM = handleCall(
                        stmt.pc, call.resolveCallTarget.toSet, newReachableMethods, calleesOfM
                    )

                case call: NonVirtualMethodCall[V] ⇒
                    calleesOfM = handleCall(
                        stmt.pc, call.resolveCallTarget.toSet, newReachableMethods, calleesOfM
                    )

                case VirtualFunctionCallStatement(call) ⇒
                    val r = handleVirtualCall(
                        method, call, call.pc, newReachableMethods, calleesOfM, virtualCallSites
                    )
                    calleesOfM = r._1
                    virtualCallSites = r._2

                case call: VirtualMethodCall[V] ⇒
                    val r = handleVirtualCall(
                        method, call, call.pc, newReachableMethods, calleesOfM, virtualCallSites
                    )
                    calleesOfM = r._1
                    virtualCallSites = r._2

                case Assignment(_, _, _: Invokedynamic[V]) | ExprStmt(_, _: Invokedynamic[V]) ⇒
                    OPALLogger.logOnce(
                        Warn(
                            "analysis",
                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                        )
                    )(p.logContext)

                case _ ⇒ //nothing to do
            }
        }
        (newInstantiatedTypes, calleesOfM, virtualCallSites)
    }

    def handleVirtualCall(
        method:              Method,
        call:                Call[V] with VirtualCall[V],
        pc:                  Int,
        newReachableMethods: ArrayBuffer[Method],
        calleesOfM:          IntMap[IntTrieSet],
        virtualCallSites:    List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]
    ): (IntMap[IntTrieSet], List[(Int /*PC*/ , ObjectType, String, MethodDescriptor)]) = {

        var resCalleesOfM = calleesOfM
        var resVirtualCallSites = virtualCallSites
        val rvs = call.receiver.asVar.value.asDomainReferenceValue.allValues
        for (rv ← rvs) {
            // for null there is no call
            if (rv.isNull.isNoOrUnknown) {
                // for precise types we can directly add the call edge here
                if (rv.isPrecise) {
                    val tgt = project.instanceCall(
                        method.classFile.thisType,
                        rv.valueType.get,
                        call.name,
                        call.descriptor
                    )
                    resCalleesOfM = handleCall(pc, tgt.toSet, newReachableMethods, resCalleesOfM)
                } else {
                    val typeBound =
                        project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                            rv.upperTypeBound
                        )
                    val receiverType =
                        if (project.classHierarchy.isSubtypeOf(typeBound, call.declaringClass).isYes)
                            typeBound
                        else
                            call.declaringClass

                    if (receiverType.isArrayType) {
                        val tgts = project.instanceCall(
                            method.classFile.thisType, receiverType, call.name, call.descriptor
                        ).toSet
                        resCalleesOfM = handleCall(pc, tgts, newReachableMethods, resCalleesOfM)
                    } else {
                        resVirtualCallSites ::= ((pc, receiverType.asObjectType, call.name, call.descriptor))
                    }
                }
            }
        }

        (resCalleesOfM, resVirtualCallSites)

    }

    def addNewReachableMethod(m: DefinedMethod, reachableMethods: ArrayBuffer[Method]): Unit = {
        if (processedMethods(declaredMethods.methodID(m)).compareAndSet(false, true))
            reachableMethods += m.definedMethod
    }

    /**
     * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
     * edges for all targets.
     */
    def handleCall(
        pc: Int, targets: Set[Method],
        newReachableMethods: ArrayBuffer[Method],
        calleesOfM:          IntMap[IntTrieSet]
    ): IntMap[IntTrieSet] = {

        var result = calleesOfM
        for {
            tgt ← targets
        } {
            val tgtDM = declaredMethods(tgt)
            val tgtId = declaredMethods.methodID(tgtDM)
            // add call edge to CG
            result = result.updated(pc, result.getOrElse(pc, IntTrieSet.empty) + tgtId)

            // the callee is now reachable and should be processed, if not done already
            addNewReachableMethod(tgtDM, newReachableMethods)
        }
        result
    }

    // modifies state
    // modifies newReachable methods
    // returns updated newCalleesOfM
    def handleVirtualCallSites(
        state:                State,
        newInstantiatedTypes: Iterator[ObjectType],
        newReachableMethods:  ArrayBuffer[Method],
        newCalleesOfM:        IntMap[IntTrieSet]
    ): IntMap[IntTrieSet] = {
        var result = newCalleesOfM
        for {
            instantiatedType ← newInstantiatedTypes // only iterate once!
            (pc, typeBound, name, descr) ← state.virtualCallSites

            if project.classHierarchy.subtypeInformation.get(typeBound).exists(_.contains(instantiatedType))
            //if project.classHierarchy.isSubtypeOf(instantiatedType, typeBound).isYes
            tgt ← project.instanceCall(
                state.method.definedMethod.classFile.thisType, instantiatedType, name, descr
            )
        } {
            val tgtID = declaredMethods(tgt)
            val tgtId = declaredMethods.methodID(tgtID)
            addNewReachableMethod(tgtID, newReachableMethods)
            // in case newCalleesOfM equals state.calleesOfM this is safe
            result = result.updated(pc, result.getOrElse(pc, IntTrieSet.empty) + tgtId)
            state.addCallEdge(pc, tgtId)
        }
        result
    }

    def partialResultForCallers(
        method:        DefinedMethod,
        newCalleesOfM: IntMap[IntTrieSet]
    ): TraversableOnce[PartialResult[DeclaredMethod, Callers]] = {
        for {
            (pc: Int, tgtsOfM: IntTrieSet) ← newCalleesOfM.iterator
            tgtID: Int ← tgtsOfM.iterator
            tgtMethod = declaredMethods(tgtID)
        } yield PartialResult[DeclaredMethod, Callers](tgtMethod, Callers.key, {
            case EPS(e, lb: Callers, ub: CallersImplementation) ⇒
                val newCallers = ub.updated(method, pc)
                if (ub != newCallers)
                    Some(EPS(e, lb, newCallers))
                else
                    None
            case EPK(e, _) ⇒
                Some(EPS(
                    tgtMethod,
                    Callers.fallback(tgtMethod, project),
                    new CallersImplementation(
                        Set(Callers.toLong(tgtID, pc)), declaredMethods
                    )
                ))
            case _ ⇒ throw new IllegalArgumentException()
        })
    }

    def partialResultForInstantiatedTypes(
        method: Method, newInstantiatedTypes: UIDSet[ObjectType]
    ): PartialResult[SomeProject, InstantiatedTypes] = {
        PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case EPS(_, lb, ub) if newInstantiatedTypes.nonEmpty ⇒
                    Some(EPS(
                        project,
                        lb,
                        ub.updated(newInstantiatedTypes)
                    ))

                case EPK(_, _) ⇒
                    Some(EPS(
                        project,
                        AllTypes,
                        InstantiatedTypes.initial(InstantiatedTypes.initialTypes ++ newInstantiatedTypes)
                    ))

                case _ ⇒ None
            })
    }

    def resultForCallees(
        instantiatedTypesEOptP: SomeEOptionP, state: State
    ): PropertyComputationResult = {
        val calleesLB = Callees.fallback(state.method, p, declaredMethods)

        // here we need a immutable copy of the current state
        val newCallees = new CalleesImplementation(state.calleesOfM, declaredMethods)
        // todo equal size or equal callees?
        if (instantiatedTypesEOptP.isFinal || newCallees.size == calleesLB.size)
            Result(state.method, newCallees)
        else {
            IntermediateResult(
                state.method,
                calleesLB,
                newCallees,
                Seq(instantiatedTypesEOptP),
                continuation(_, state)
            )
        }
    }
}

object EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new RTACallGraphAnalysis(project)

        val mainDescriptor = MethodDescriptor.JustTakes(ArrayType(ObjectType.String))
        // TODO also handle libraries
        val entryPoints: Seq[Method] = project.allMethodsWithBody.filter { m ⇒
            m.name == "main" && m.descriptor == mainDescriptor && m.isStatic && m.body.isDefined
        }
        println(s"number of entrypoints: ${entryPoints.size}")
        if (entryPoints.isEmpty)
            OPALLogger.logOnce(
                Error("analysis", "the project has no entry points")
            )(project.logContext)

        // ensure that an entry point is not scheduled later on again
        entryPoints.foreach(entrypoint ⇒ {
            val declaredMethods = project.get(DeclaredMethodsKey)
            val dm = declaredMethods(entrypoint)
            if (!analysis.processedMethods(declaredMethods.methodID(dm)).compareAndSet(false, true))
                throw new IllegalStateException("Unexpected modification of processedMethods array")
        })

        val declaredMethods = project.get(DeclaredMethodsKey)

        propertyStore.scheduleEagerComputationsForEntities(
            entryPoints.map(declaredMethods.apply)
        )(analysis.step1)
        analysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes, Callers, Callees)
}
