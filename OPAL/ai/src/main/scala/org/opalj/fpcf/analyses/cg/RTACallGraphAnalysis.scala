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
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.MethodIDKey
import org.opalj.br.analyses.ProjectLike
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.CallGraph
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.log.Error
import org.opalj.log.OPALLogger
import org.opalj.log.Warn
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.GetStatic
import org.opalj.tac.Invokedynamic
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutStatic
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall

import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class State(
        method:                              Method,
        virtualCallSites:                    Set[(Int /*PC*/ , ReferenceType, String, MethodDescriptor)],
        var calleesOfM:                      Map[Int, Set[Method]],
        var currentIndexOfInstantiatedTypes: Int
) {
    def addCallEdge(pc: Int, tgt: Method): Unit = {
        calleesOfM = calleesOfM.updated(pc, calleesOfM.getOrElse(pc, Set.empty) + tgt)
    }

}

/**
 * TODO
 *
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] val methodIds = project.get(MethodIDKey)

    val processedMethods: Array[AtomicBoolean] = {
        Array.fill(project.allMethods.size) { new AtomicBoolean }
    }

    def addNewReachableMethod(m: Method, reachableMethods: ArrayBuffer[Method]): Unit = {
        if (processedMethods(methodIds(m)).compareAndSet(false, true))
            reachableMethods += m
    }

    def updatedCG(
        callGraph: CallGraph, method: Method, calleesOfM: Map[Int, Set[Method]]
    ): CallGraph = {
        val CallGraph(callees, callers) = callGraph

        var newCallees = callees // todo with default
        calleesOfM.foreach {
            case (pc, tgtsOfM) ⇒
                newCallees = newCallees.updated(method, newCallees.getOrElse(method, Map.empty[Int, Set[Method]]).updated(pc, newCallees.getOrElse(method, Map.empty[Int, Set[Method]]).getOrElse(pc, Set.empty) ++ tgtsOfM))
        }

        var newCallers = callers

        calleesOfM.foreach {
            case (pc, tgtsOfM) ⇒
                tgtsOfM.foreach { tgt ⇒
                    newCallers = newCallers.updated(tgt, newCallers(tgt) + (method → pc))
                }
        }
        CallGraph(newCallees, newCallers)
    }

    def step2(
        instantiatedTypesEOptP: EOptionP[SomeProject, InstantiatedTypes],
        state:                  State
    ): PropertyComputationResult = {
        val method = state.method

        val newInstantiatedTypes =
            instantiatedTypesEOptP match {
                case EPS(_, lb: InstantiatedTypes, ub: InstantiatedTypes) ⇒
                    ub.orderedTypes.drop(state.currentIndexOfInstantiatedTypes)
                case _ ⇒
            }
        val newReachableMethods = ArrayBuffer.empty[Method]
        val newCallTargets = mutable.Map.empty[Int /*PC*/ , Set[Method]].withDefaultValue(Set.empty)

        for {
            (pc, typeBound, name, descr) ← state.virtualCallSites
            instantiatedType ← newInstantiatedTypes
        } {
            if (project.classHierarchy.isSubtypeOf(instantiatedType, typeBound).isYesOrUnknown)
                // todo is this correct method
                project.resolveMethodReference(instantiatedType, name, descr).foreach { tgt ⇒
                    addNewReachableMethod(tgt, newReachableMethods)
                    newCallTargets(pc) += tgt
                    state.addCallEdge(pc, tgt)
                }
        }

        IncrementalResult(
            Results(
                if (instantiatedTypesEOptP.isFinal)
                    Result(state.method, Callees(state.calleesOfM))
                else {
                    val calleesLB = Callees(CallGraph.fallbackCG(p).callees(state.method))
                    IntermediateResult(
                        state.method,
                        calleesLB,
                        Callees(state.calleesOfM),
                        Seq(instantiatedTypesEOptP),
                        step2(_, state)
                    )
                },
                PartialResult(project, CallGraph.key, {
                    case EPS(_, _, ub) if newCallTargets.nonEmpty ⇒
                        // add all new edges to ub
                        // assert(newCG.size > ub.size)
                        None
                    case EPK(_, _) ⇒
                        var callers = Map.empty[Method, Set[(Method, Int)]].withDefaultValue(Set.empty)
                        newCallTargets.foreach {
                            case (pc, tgtsOfM) ⇒
                                tgtsOfM.foreach { tgt ⇒
                                    callers = callers.updated(tgt, callers(tgt) + (method → pc))
                                }
                        }
                        Some(EPS(
                            project,
                            CallGraph.fallbackCG(p),
                            new CallGraph(Map(method → newCallTargets), callers)
                        ))
                    case _ ⇒ None
                })
            ),
            newReachableMethods.map(nextMethod ⇒ (doComputeCG _, nextMethod))
        )
    }

    def doComputeCG(
        method: Method
    ): PropertyComputationResult = {

        val methodID = methodIds(method)
        assert(processedMethods(methodID).get())

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = Set.empty[ObjectType]

        // the set of methods that become reachable due to the current method and instantiated types
        val newReachableMethods = ArrayBuffer.empty[Method]

        // in case the instantiatedTypes are not finally computed, we depend on them
        var instantiatedTypesDependee: Option[EOptionP[ProjectLike, InstantiatedTypes]] = None

        def addClInitAsNewReachable(objectType: ObjectType): Unit = {
            project.classHierarchy.allSupertypes(objectType, reflexive = true) foreach { x ⇒
                project.classFile(x).foreach { cf ⇒
                    cf.staticInitializer.foreach(addNewReachableMethod(_, newReachableMethods))
                }
            }
        }

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        var currentInstantiatedTypesCount = 0
        val instantiatedTypesUB: Set[ObjectType] = instantiatedTypesEOptP match {
            case FinalEP(_, ub) ⇒
                currentInstantiatedTypesCount = ub.types.size
                ub.types
            case eps @ EPS(_, _ /*lb*/ , ub: InstantiatedTypes) ⇒
                instantiatedTypesDependee = Some(eps)
                currentInstantiatedTypesCount = ub.types.size
                ub.types

            case epk ⇒
                instantiatedTypesDependee = Some(epk)
                addClInitAsNewReachable(ObjectType.String)
                // todo initial leave types should be also added
                Set(ObjectType.String) //TODO use default types depending on project setting
        }

        val tac = tacaiProvider(method)

        // for each call site in the current method, the set of methods that might called
        val calleesOfM = mutable.OpenHashMap.empty[Int /*PC*/ , Set[Method]].withDefaultValue(Set.empty)

        /**
         * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
         * edges for all targets of instantiatedTypes.
         */
        def handleCall(pc: Int, targets: Set[Method]): Unit = {
            for {
                tgt ← targets
            } {
                // add call edge to CG
                calleesOfM(pc) += tgt

                // the callee is now reachable and should be processed, if not done already
                addNewReachableMethod(tgt, newReachableMethods)
            }
        }

        implicit val p: SomeProject = project

        var virtualCallSites: Set[(Int /*PC*/ , ReferenceType, String, MethodDescriptor)] = Set.empty
        // for allocation sites, add new types
        // for calls, add new edges
        for (stmt ← tac.stmts) {
            stmt match {
                case Assignment(_, _, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                        addClInitAsNewReachable(allocatedType)
                    }

                case ExprStmt(_, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType)) {
                        newInstantiatedTypes += allocatedType
                        addClInitAsNewReachable(allocatedType)
                    }

                case Assignment(_, _, GetStatic(_, declaringClass, _, _)) ⇒
                    addClInitAsNewReachable(declaringClass)

                case ExprStmt(_, GetStatic(_, declaringClass, _, _)) ⇒
                    addClInitAsNewReachable(declaringClass)

                case PutStatic(_, declaringClass, _, _, _) ⇒
                    addClInitAsNewReachable(declaringClass)

                case StaticFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case call: StaticMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case NonVirtualFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case call: NonVirtualMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case VirtualFunctionCallStatement(call) ⇒
                    val typeBound = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                        call.receiver.asVar.value.asDomainReferenceValue.upperTypeBound
                    )
                    virtualCallSites += ((call.pc, typeBound, call.name, call.descriptor))

                case call: VirtualMethodCall[V] ⇒
                    val typeBound = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                        call.receiver.asVar.value.asDomainReferenceValue.upperTypeBound
                    )

                    // todo if typebound is leaf (e.g. method is final) -> compute directly

                    virtualCallSites += ((call.pc, typeBound, call.name, call.descriptor))

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

        var results: Set[PropertyComputationResult] = Set.empty

        // todo maybe determine locally
        val calleesLB = Callees(CallGraph.fallbackCG(p).callees(method))
        val newCallees = Callees(calleesOfM)

        // callee updates (in order to hold a dependency to instantiated types)
        results += (
            if (instantiatedTypesDependee.isEmpty || calleesLB == newCallees) {
                Result(method, newCallees)
            } else
                IntermediateResult(
                    method,
                    calleesLB,
                    newCallees,
                    instantiatedTypesDependee.toSeq,
                    continuation(_, state)
                )
        )
        // instantiated types updates

        results += PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
            {
                case EPS(_, lb, ub) if newInstantiatedTypes.nonEmpty ⇒
                    Some(EPS(
                        project,
                        lb,
                        InstantiatedTypes(ub.types ++ newInstantiatedTypes)
                    ))

                case EPK(_, _) ⇒
                    println(method)
                    Some(EPS(
                        project,
                        InstantiatedTypes.allTypes(p),
                        InstantiatedTypes(instantiatedTypesUB ++ newInstantiatedTypes)
                    ))

                case _ ⇒ None
            })

        // call graph updates
        results += PartialResult[SomeProject, CallGraph](p, CallGraph.key, {
            case EPS(_, lb: CallGraph, ub: CallGraph) ⇒

                val newCG = updatedCG(ub, method, calleesOfM)

                if (newCG.size == ub.size)
                    None
                else
                    Some(EPS(project, lb, newCG))

            case EPK(_, _) ⇒
                var callers = Map.empty[Method, Set[(Method, Int)]].withDefaultValue(Set.empty)
                calleesOfM.foreach {
                    case (pc, tgtsOfM) ⇒
                        tgtsOfM.foreach { tgt ⇒
                            callers = callers.updated(tgt, callers(tgt) + (method → pc))
                        }
                }
                Some(EPS(
                    project,
                    CallGraph.fallbackCG(p),
                    new CallGraph(Map(method → calleesOfM), callers)
                ))
        })

        IncrementalResult(
            Results(results),
            // continue the computation with the newly reachable methods
            newReachableMethods.map(nextMethod ⇒ (doComputeCG _, nextMethod))
        )
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
            val methodIds = project.get(MethodIDKey)
            if (!analysis.processedMethods(methodIds(entrypoint)).compareAndSet(false, true))
                throw new IllegalStateException("Unexpected modification of processedMethods array")
        })

        propertyStore.scheduleEagerComputationsForEntities(entryPoints)(analysis.doComputeCG)
        analysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes, CallGraph, Callees)
}
