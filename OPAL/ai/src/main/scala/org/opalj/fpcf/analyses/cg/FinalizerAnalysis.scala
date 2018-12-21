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
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.fpcf.cg.properties.OnlyVMLevelCallers
import org.opalj.fpcf.cg.properties.VMReachableFinalizers

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
    ): ProperPropertyComputationResult = {
        var instantiatedTypesDependee: Option[SomeEPS] = None
        val (newFinalizers, results) = eps match {
            case FinalP(instantiatedTypes: InstantiatedTypes) ⇒
                handleNewInstantiatedTypes(instantiatedTypes)
            case eps @ InterimUBP(ub: InstantiatedTypes) ⇒
                instantiatedTypesDependee = Some(eps)
                handleNewInstantiatedTypes(ub)
        }

        state.addFinalizers(newFinalizers)

        val result = if (instantiatedTypesDependee.isEmpty)
            Result(p, new VMReachableFinalizers(state.vmReachableFinalizers))
        else InterimResult(
            InterimEUBP(p, new VMReachableFinalizers(state.vmReachableFinalizers)),
            instantiatedTypesDependee,
            handleInstantiatedTypesUpdate
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
                            case InterimUBP(ub) if !ub.hasVMLevelCallers ⇒
                                Some(InterimEUBP(finalizer, ub.updatedWithVMLevelCall()))

                            case _: InterimEP[DeclaredMethod, CallersProperty] ⇒ None

                            case _: EPK[DeclaredMethod, CallersProperty] ⇒
                                Some(InterimEUBP(finalizer, OnlyVMLevelCallers))

                            case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
                        })

                        (finalizersR + finalizer.id) → (result +: resultsR)

                    } else {
                        (finalizersR, resultsR)
                    }
            }
    }

}

object EagerFinalizerAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(InstantiatedTypes)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(VMReachableFinalizers),
        PropertyBounds.ub(CallersProperty)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FinalizerAnalysis = {
        val analysis = new FinalizerAnalysis(p)
        ps.registerTriggeredComputation(InstantiatedTypes.key, analysis.analyze)
        analysis
    }
}
