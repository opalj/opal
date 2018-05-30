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
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.MethodIDKey
import org.opalj.br.analyses.ProjectLike
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.CallGraph
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualFunctionCallStatement
import org.opalj.tac.VirtualMethodCall

class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    private[this] val tacaiProvider = project.get(SimpleTACAIKey)
    private[this] val methodIds = project.get(MethodIDKey)

    var processedMethods = new Array[Boolean](Int.MaxValue)

    def doComputation(m: Method): PropertyComputationResult = {
        var instantiatedTypesDependee: Option[EOptionP[ProjectLike, InstantiatedTypes]] = None

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesUB: Set[ObjectType] = propertyStore(project, InstantiatedTypes.key) match {
            case FinalEP(_, ub) ⇒ ub.types
            case eps @ EPS(_, _ /*lb*/ , ub: InstantiatedTypes) ⇒
                instantiatedTypesDependee = Some(eps)
                ub.types

            case epk ⇒
                instantiatedTypesDependee = Some(epk)
                Set.empty
        }

        val tac = tacaiProvider(m)

        var newInstantiatedTypes = Set.empty[ObjectType] //TODO maybe use Seq instead
        var newMethods = Set.empty[Method] //TODO maybe use Seq instead
        var callees = Map.empty[Method, Map[Int /*PC*/ , Set[Method]]] //TODO use withDefault
        var callers = Map.empty[Method, Set[(Method, Int /*PC*/ )]] //TODO use withDefault

        def handleCall(pc: Int, tgts: Set[Method]): Unit = {
            for (tgt ← tgts) {
                val methodId = methodIds(tgt)
                // add call edge to CG
                callees = callees.updated(m, callees(m).updated(pc, callees(m)(pc) + tgt))
                callers = callers.updated(tgt, callers(tgt) + (m → pc))

                // the callee is now reachable and should be processed, if not done already
                if (!processedMethods(methodId)) {
                    newMethods += tgt
                    processedMethods(methodId) = true
                }
            }
        }

        for (stmt ← tac.stmts) {
            stmt match {
                case Assignment(_, _, New(_, allocatedType)) ⇒
                    if (!instantiatedTypesUB.contains(allocatedType))
                        newInstantiatedTypes += allocatedType

                case StaticFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget(p).toSet)

                case call: StaticMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget(p).toSet)

                case NonVirtualFunctionCallStatement(call) ⇒
                    handleCall(stmt.pc, call.resolveCallTarget(p).toSet)

                case call: NonVirtualMethodCall[V] ⇒
                    handleCall(stmt.pc, call.resolveCallTarget(p).toSet)

                case VirtualFunctionCallStatement(call) ⇒
                //TODO I need "ev" here: call.resolveCallTargets(m.classFile.thisType)

                case call: VirtualMethodCall[V]         ⇒
                //TODO I need "ev" here: call.resolveCallTargets(m.classFile.thisType)

            }
        }

        // TODO how to register dependencies?
        IncrementalResult(
            Results(
                Traversable(
                    // instantiated types updates
                    PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
                        (eOptionP: EOptionP[SomeProject, InstantiatedTypes]) ⇒ eOptionP match {
                            case EPS(_, lb, ub) ⇒
                                Some(EPS(
                                    p,
                                    lb,
                                    InstantiatedTypes(ub.types ++ newInstantiatedTypes)
                                ))
                            case EPK(_, _) ⇒
                                Some(EPS(
                                    p,
                                    InstantiatedTypes.allTypes(p),
                                    InstantiatedTypes(newInstantiatedTypes)
                                ))
                        }),

                    // TODO handle the update of a call grpah
                    PartialResult[SomeProject, CallGraph](p, CallGraph.key, {
                        case EPS(_, lb: CallGraph, ub: CallGraph) ⇒
                            Some(EPS(p, lb, ub)) //TODO compute the union between the ub and the computed cg
                            //TODO it should be ub except the edges from/to m and instead the computed ones here
                        case EPK(_, _) ⇒ Some(EPS(
                            p, CallGraph.fallbackCG(p), new CallGraph(callees, callers)
                        ))
                    })
                )
            ),
            // continue the computation with the newly reachable methods
            newMethods.map(nextMethod ⇒ (doComputation _, nextMethod))
        )
    }
}

class EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new RTACallGraphAnalysis(project)
        val entryPoints: Seq[Method] = Seq.empty //TODO
        propertyStore.scheduleEagerComputationsForEntities(entryPoints)(analysis.doComputation)
        analysis
    }

    override def uses: Set[PropertyKind] = Set(InstantiatedTypes /* TODO maybe also: ,CallGraph*/ )

    override def derives: Set[PropertyKind] = Set(InstantiatedTypes, CallGraph)
}
