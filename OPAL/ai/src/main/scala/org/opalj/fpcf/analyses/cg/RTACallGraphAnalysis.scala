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

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ArrayType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.MethodIDKey
import org.opalj.br.analyses.ProjectLike
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.CallGraph
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall

/**
 * TODO
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] val methodIds = project.get(MethodIDKey)

    var processedMethods = new Array[Boolean](Int.MaxValue)

    def computeCG(m: Method): PropertyComputationResult = {
        doComputeCG(m, propertyStore(project, InstantiatedTypes.key))
    }

    def doComputeCG(
        method: Method, instantiatedTypes: EOptionP[SomeProject, InstantiatedTypes]
    ): PropertyComputationResult = {

        // in case the instantiatedTypes are not finally computed, we depend on them
        var instantiatedTypesDependee: Option[EOptionP[ProjectLike, InstantiatedTypes]] = None

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesUB: Set[ObjectType] = instantiatedTypes match {
            case FinalEP(_, ub) ⇒ ub.types
            case eps @ EPS(_, _ /*lb*/ , ub: InstantiatedTypes) ⇒
                instantiatedTypesDependee = Some(eps)
                ub.types

            case epk ⇒
                instantiatedTypesDependee = Some(epk)
                Set(ObjectType.String) //TODO use default types depending on project setting
        }

        val tac = tacaiProvider(method)

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = Set.empty[ObjectType]

        // the set of methods that become reachable due to the current method and instantiated types
        var newReachableMethods = Set.empty[Method]

        // for each call site in the current method, the set of methods that might called
        var calleesOfM = Map.empty[Int /*PC*/ , Set[Method]].withDefaultValue(Set.empty)

        // for each method that might be called by the current method, the set of the callsites in m
        var callers = Map.empty[Method, Set[(Method, Int /*PC*/ )]].withDefaultValue(Set.empty)

        /**
         * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
         * edges for all targets of instantiatedTypes.
         */
        def handleCall(pc: Int, targets: Set[Method]): Unit = {
            for {
                tgt ← targets
                if instantiatedTypesUB.contains(tgt.classFile.thisType)
            } {
                val methodId = methodIds(tgt)
                // add call edge to CG
                calleesOfM = calleesOfM.updated(pc, calleesOfM(pc) + tgt)
                callers = callers.updated(tgt, callers(tgt) + (method → pc))

                // the callee is now reachable and should be processed, if not done already
                if (!processedMethods(methodId)) {
                    newReachableMethods += tgt
                    processedMethods(methodId) = true
                }
            }
        }

        implicit val p: SomeProject = project

        // for allocation sites, add new types
        // for calls, add new edges
        for (stmt ← tac.stmts) {
            stmt match {
                case Assignment(_, _, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType))
                        newInstantiatedTypes += allocatedType

                case StaticFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case call: StaticMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case NonVirtualFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case call: NonVirtualMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget.toSet)

                case call: VirtualMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTargets(method.classFile.thisType).toSet)

            }
        }

        // for the newly instantiated types we need to ensure that clinit is called
        val cfInits = newInstantiatedTypes.flatMap { newType ⇒
            classHierarchy.allSupertypes(newType, reflexive = true).flatMap { t ⇒
                p.classFile(t) match {
                    case Some(cf) ⇒ cf.staticInitializer
                    case None     ⇒ None
                }
            }
        }
        for (cfInit ← cfInits) {
            val methodId = methodIds(cfInit)
            // the initializer is now reachable and should be processed, if not done already
            if (!processedMethods(methodId)) {
                newReachableMethods += cfInit
                processedMethods(methodId) = true
            }
        }

        def continuation(eps: SomeEPS): PropertyComputationResult = {
            eps match {
                case newInstantiatedTypes: EPS[SomeProject, InstantiatedTypes] ⇒
                    doComputeCG(method, newInstantiatedTypes)
            }
        }

        IncrementalResult(
            Results(
                Traversable(
                    // instantiated types updates
                    PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
                        (eOptionP: EOptionP[SomeProject, InstantiatedTypes]) ⇒ eOptionP match {
                            case EPS(_, lb, ub) ⇒
                                Some(EPS(
                                    project,
                                    lb,
                                    InstantiatedTypes(ub.types ++ newInstantiatedTypes)
                                ))
                            case EPK(_, _) ⇒
                                Some(EPS(
                                    project,
                                    InstantiatedTypes.allTypes(p),
                                    InstantiatedTypes(newInstantiatedTypes)
                                ))
                        }),

                    // call graph updates
                    PartialResult[SomeProject, CallGraph](p, CallGraph.key, {
                        case EPS(_, lb: CallGraph, CallGraph(calleesUB, callersUB)) ⇒
                            val callees = calleesUB.updated(method, calleesOfM)
                            val newCallers = callers.foldLeft(callersUB) {
                                case (tmpCG, (caller, newEdges)) ⇒
                                    tmpCG.updated(caller, tmpCG(caller) ++ newEdges)
                            }

                            Some(EPS(project, lb, CallGraph(callees, newCallers)))
                        case EPK(_, _) ⇒ Some(EPS(
                            project,
                            CallGraph.fallbackCG(p),
                            new CallGraph(Map(method → calleesOfM), callers)
                        ))
                    }),

                    // callee updates (in order to hold a dependency to instantiated types)
                    if (instantiatedTypesDependee.isEmpty)
                        Result(method, Callees(calleesOfM))
                    else
                        IntermediateResult(
                            method,
                            CallGraph.fallbackCG(p).callees(method),
                            calleesOfM,
                            instantiatedTypesDependee.toSeq,
                            continuation
                        )
                )
            ),
            // continue the computation with the newly reachable methods
            newReachableMethods.map(nextMethod ⇒ (computeCG _, nextMethod))
        )
    }
}

class EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new RTACallGraphAnalysis(project)
        val mainDescriptor = MethodDescriptor.JustTakes(ArrayType(ObjectType.String))
        // TODO also handle libraries
        val entryPoints: Seq[Method] = project.allMethodsWithBody.filter { m ⇒
            m.name == "main" && m.descriptor.parameterTypes == mainDescriptor
        }
        propertyStore.scheduleEagerComputationsForEntities(entryPoints)(analysis.computeCG)
        analysis
    }

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes /* TODO maybe also: ,CallGraph*/ )

    override def derives: Set[PropertyKind] = Set(InstantiatedTypes, CallGraph)
}
