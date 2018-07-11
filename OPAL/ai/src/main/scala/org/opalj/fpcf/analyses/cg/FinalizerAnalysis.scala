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

import org.opalj.br.DeclaredMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.Callees
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.LowerBoundCallers
import org.opalj.fpcf.properties.OnlyVMLevelCallers
import org.opalj.fpcf.properties.VMReachableFinalizers
import org.opalj.fpcf.properties.VMReachableFinalizersFallback

class FinalizerAnalysisState(var seenTypes: Int, private[this] var _vmReachableFinalizers: IntTrieSet) {
    def addFinalizer(finalizer: DeclaredMethod)(implicit declaredMethods: DeclaredMethods): Unit = {
        _vmReachableFinalizers += declaredMethods.methodID(finalizer)
    }

    def addFinalizers(finalizers: IntTrieSet): Unit = {
        _vmReachableFinalizers ++= finalizers
    }

    def vmReachableFinalizers: IntTrieSet = _vmReachableFinalizers
}

class FinalizerAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(p: SomeProject): PropertyComputationResult = {

        implicit val state: FinalizerAnalysisState = new FinalizerAnalysisState(0, IntTrieSet.empty)
        val instantiatedTypesResult: EOptionP[SomeProject, InstantiatedTypes] = propertyStore(p, InstantiatedTypes.key)

        handleInstantiatedTypesUpdate(instantiatedTypesResult.asEPS)
    }

    def handleInstantiatedTypesUpdate(eps: SomeEPS)(
        implicit
        state: FinalizerAnalysisState
    ): PropertyComputationResult = {
        var instantiatedTypesDependee: Option[SomeEPS] = None
        val (newFinalizers, results) = eps match {
            case FinalEP(_, instantiatedTypes: InstantiatedTypes) ⇒
                handleNewInstantiatedTypes(instantiatedTypes)
            case eps @ IntermediateEP(_, _, ub: InstantiatedTypes) ⇒
                instantiatedTypesDependee = Some(eps)
                handleNewInstantiatedTypes(ub)
        }

        state.addFinalizers(newFinalizers)

        val result = if (instantiatedTypesDependee.isEmpty)
            Result(p, new VMReachableFinalizers(state.vmReachableFinalizers))
        else IntermediateResult(
            p,
            VMReachableFinalizersFallback,
            new VMReachableFinalizers(state.vmReachableFinalizers),
            instantiatedTypesDependee, handleInstantiatedTypesUpdate
        )

        Results(results ++ Iterator(result))
    }

    def handleNewInstantiatedTypes(
        instantiatedTypes: InstantiatedTypes
    )(
        implicit
        state: FinalizerAnalysisState
    ): (IntTrieSet, Traversable[PartialResult[DeclaredMethod, CallersProperty]]) = {
        instantiatedTypes.getNewTypes(state.seenTypes).foldLeft(
            (IntTrieSet.empty, List.empty[PartialResult[DeclaredMethod, CallersProperty]])
        ) {
                case ((finalizersR, resultsR), newInstantiatedType) ⇒
                    val finalizers = project.resolveAllMethodReferences(
                        newInstantiatedType,
                        "finalize",
                        MethodDescriptor.NoArgsAndReturnVoid
                    )
                    if (finalizers.size == 1) {
                        val finalizer = declaredMethods(finalizers.head)
                        val result = PartialResult[DeclaredMethod, CallersProperty](finalizer, CallersProperty.key, {
                            case EPK(e, _) ⇒ Some(
                                EPS(e, new LowerBoundCallers(project, e), OnlyVMLevelCallers)
                            )
                            case EPS(e, lb, ub) if !ub.hasVMLevelCallers ⇒
                                Some(EPS(e, lb, ub.updateVMLevelCall()))

                            case _ ⇒ None
                        })

                        (finalizersR + declaredMethods.methodID(finalizer)) → (result +: resultsR)

                    } else {
                        (finalizersR, resultsR)
                    }
            }
    }

}

object EagerFinalizerAnalysisScheduler extends FPCFEagerAnalysisScheduler {

    override type InitializationData = FinalizerAnalysis

    override def start(
        project: SomeProject, propertyStore: PropertyStore, finalizerAnalysis: FinalizerAnalysis
    ): FPCFAnalysis = {
        finalizerAnalysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(InstantiatedTypes, CallersProperty, Callees)

    override def init(p: SomeProject, ps: PropertyStore): FinalizerAnalysis = {
        val analysis = new FinalizerAnalysis(p)
        ps.registerTriggeredComputation(InstantiatedTypes.key, analysis.analyze)
        analysis
    }
}
