/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.LowerBoundCallers
import org.opalj.fpcf.properties.OnlyVMLevelCallers
import org.opalj.fpcf.properties.VMReachableFinalizers
import org.opalj.fpcf.properties.VMReachableFinalizersFallback

/**
 * Computes the set of finalize methods that are being called by the VM during the execution of the
 * `project`.
 * Extends the call graph analysis (e.g. [[RTACallGraphAnalysis]]) to include the calls to these
 * methods.
 *
 * @author Florian Kuebler
 */
class FinalizerAnalysisState(var seenTypes: Int, private[this] var _vmReachableFinalizers: IntTrieSet) {
    def addFinalizer(finalizer: DeclaredMethod): Unit = {
        _vmReachableFinalizers += finalizer.id
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

        implicit val state: FinalizerAnalysisState =
            new FinalizerAnalysisState(0, IntTrieSet.empty)

        val instantiatedTypesResult: EOptionP[SomeProject, InstantiatedTypes] =
            propertyStore(p, InstantiatedTypes.key)

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
        val index = state.seenTypes
        state.seenTypes = instantiatedTypes.numElements
        instantiatedTypes.getNewTypes(index).foldLeft(
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
                                EPS(e, LowerBoundCallers, OnlyVMLevelCallers)
                            )
                            case EPS(e, lb, ub) if !ub.hasVMLevelCallers ⇒
                                Some(EPS(e, lb, ub.updatedWithVMLevelCall()))

                            case _ ⇒ None
                        })

                        (finalizersR + finalizer.id) → (result +: resultsR)

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

    override def derives: Predef.Set[PropertyKind] = Predef.Set(VMReachableFinalizers, CallersProperty)

    override def init(p: SomeProject, ps: PropertyStore): FinalizerAnalysis = {
        val analysis = new FinalizerAnalysis(p)
        ps.registerTriggeredComputation(InstantiatedTypes.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
