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
import org.opalj.tac.Invokedynamic
import org.opalj.tac.New
import org.opalj.tac.NonVirtualFunctionCallStatement
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.StaticFunctionCallStatement
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall

import scala.collection.Map
import scala.collection.Set
import scala.collection.mutable

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

    val processedMethods: Array[AtomicBoolean] = {
        Array.fill(project.allMethods.size) { new AtomicBoolean }
    }

    def computeCG(m: Method): PropertyComputationResult = {
        val methodID = methodIds(m)

        if (processedMethods(methodID).compareAndSet(false, true))
            doComputeCG(m, propertyStore(project, InstantiatedTypes.key))
        else
            NoResult
    }

    def doComputeCG(
        method: Method, instantiatedTypes: EOptionP[SomeProject, InstantiatedTypes]
    ): PropertyComputationResult = {

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // the set of types for which we find an allocation which was not present before
        var newInstantiatedTypes = Set.empty[ObjectType]

        // the set of methods that become reachable due to the current method and instantiated types
        var newReachableMethods = Set.empty[Method]

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
                project.classFile(ObjectType.String).foreach { cf ⇒
                    cf.staticInitializer.foreach { clInit ⇒
                        if (processedMethods(methodIds(clInit)).compareAndSet(false, true))
                            newReachableMethods += clInit
                    }
                }

                Set(ObjectType.String) //TODO use default types depending on project setting
        }

        val tac = tacaiProvider(method)

        // for each call site in the current method, the set of methods that might called
        val calleesOfM = mutable.OpenHashMap.empty[Int /*PC*/ , Set[Method]].withDefaultValue(Set.empty)

        // for each method that might be called by the current method, the set of the callsites in m
        val callers = mutable.AnyRefMap.empty[Method, Set[(Method, Int /*PC*/ )]].withDefaultValue(Set.empty)

        /**
         * For a call at `pc` and the set of `targets` (determined by CHA), add corresponding
         * edges for all targets of instantiatedTypes.
         */
        def handleCall(pc: Int, targets: Set[Method]): Unit = {
            for {
                tgt ← targets
            } {
                val methodId = methodIds(tgt)
                // add call edge to CG
                calleesOfM(pc) += tgt
                callers(tgt) += (method → pc)

                // the callee is now reachable and should be processed, if not done already

                if (processedMethods(methodId).compareAndSet(false, true)) {
                    newReachableMethods += tgt
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
                    val tgts = call.resolveCallTargets(method.classFile.thisType)

                    // TODO it seems to be the case that there is a bug in the tac code
                    handleCall(
                        stmt.pc,
                        tgts.filter(tgt ⇒ instantiatedTypesUB.contains(tgt.classFile.thisType))
                    )

                case Assignment(_, _, _: Invokedynamic[V]) ⇒
                    OPALLogger.logOnce(
                        Warn(
                            "analysis",
                            "unresolved invokedynamics are not handled. please use appropriate reading configuration"
                        )
                    )(p.logContext)

                case _ ⇒ //nothing to do

            }
        }

        // for the newly instantiated types we need to ensure that clinit is called
        val clInits = newInstantiatedTypes.flatMap { newType ⇒
            classHierarchy.allSupertypes(newType, reflexive = true).flatMap { t ⇒
                p.classFile(t) match {
                    case Some(cf) ⇒ cf.staticInitializer
                    case None     ⇒ None
                }
            }
        }
        for (clInit ← clInits) {
            val methodId = methodIds(clInit)
            // the initializer is now reachable and should be processed, if not done already
            if (processedMethods(methodId).compareAndSet(false, true)) {
                newReachableMethods += clInit
            }

        }
        var results: Set[PropertyComputationResult] = Set.empty

        def continuation(eps: SomeEPS): PropertyComputationResult = {
            doComputeCG(method, eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]])
        }

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
                    continuation
                )
        )

        // instantiated types updates
        if (newInstantiatedTypes.nonEmpty)
            results += PartialResult[SomeProject, InstantiatedTypes](p, InstantiatedTypes.key,
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
                })

        // call graph updates
        results += PartialResult[SomeProject, CallGraph](p, CallGraph.key, {
            case EPS(_, lb: CallGraph, ub @ CallGraph(calleesUB, callersUB)) ⇒
                val newCallees = calleesUB.updated(method, calleesOfM)
                val newCallers = callers.foldLeft(callersUB) {
                    case (tmpCG, (caller, newEdges)) ⇒
                        tmpCG.updated(caller, tmpCG(caller) ++ newEdges)
                }

                val newCG = CallGraph(newCallees, newCallers)

                if (newCG.size == ub.size)
                    None
                else
                    Some(EPS(project, lb, newCG))

            case EPK(_, _) ⇒ Some(EPS(
                project,
                CallGraph.fallbackCG(p),
                new CallGraph(Map(method → calleesOfM), callers)
            ))
        })

        IncrementalResult(
            Results(results),
            // continue the computation with the newly reachable methods
            newReachableMethods.map(nextMethod ⇒ (computeCG _, nextMethod))
        )
    }
}

object EagerRTACallGraphAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new RTACallGraphAnalysis(project)

        val mainDescriptor = MethodDescriptor.JustTakes(ArrayType(ObjectType.String))
        // TODO also handle libraries
        val entryPoints: Seq[Method] = project.allMethodsWithBody.filter { m ⇒
            m.name == "main" && m.descriptor == mainDescriptor && m.body.isDefined
        }
        if (entryPoints.isEmpty)
            OPALLogger.logOnce(
                Error("analysis", "the project has no entry points")
            )(project.logContext)

        propertyStore.scheduleEagerComputationsForEntities(entryPoints)(analysis.computeCG)
        analysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes, CallGraph, Callees)
}
