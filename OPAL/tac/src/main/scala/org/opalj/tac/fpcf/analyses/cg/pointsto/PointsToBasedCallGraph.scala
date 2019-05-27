/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.cg.pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.Call
import org.opalj.tac.VirtualCall
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysis
import org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DirectCalls
import org.opalj.tac.fpcf.analyses.pointsto.PointsToBasedAnalysis

/**
 * Uses the [[org.opalj.br.fpcf.pointsto.properties.PointsTo]] of
 * [[org.opalj.tac.common.DefinitionSite]] and[[org.opalj.br.analyses.VirtualFormalParameter]]s
 * in order to determine the targets of virtual method calls.
 *
 * @author Florian Kuebler
 */
class PointsToBasedCallGraph private[analyses] (
        final val project: SomeProject
) extends CallGraphAnalysis with PointsToBasedAnalysis {

    override type State = PointsToBasedCGState

    /**
     * Computes the calles of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    override def handleImpreciseCall(
        caller: DefinedMethod,
        call:   Call[V] with VirtualCall[V],
        pc:     Int, specializedDeclaringClassType: ReferenceType,
        potentialTargets:  ForeachRefIterator[ObjectType],
        calleesAndCallers: DirectCalls
    )(implicit state: PointsToBasedCGState): Unit = {
        val callerType = caller.definedMethod.classFile.thisType
        val callSite = (pc, call.name, call.descriptor, call.declaringClass)
        val pointsToSet = handleDefSites(callSite, call.receiver.asVar.definedBy)

        var types = IntTrieSet.empty

        for (newType ← potentialTargets) {
            if (pointsToSet.contains(newType)) {
                val tgtR = project.instanceCall(
                    callerType,
                    newType,
                    call.name,
                    call.descriptor
                )
                handleCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            } else {
                types += newType.id
            }
        }
        state.initialPotentialTypesOfCallSite(callSite, types)

    }

    override def c(
        state: PointsToBasedCGState
    )(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case EUBPS(e, ub: PointsTo, isFinal) ⇒
            val relevantCallSites = state.callSitesForDefSite(e)

            // ensures, that we only add new calls
            val calls = new DirectCalls()

            for (callSite ← relevantCallSites) {
                val oldEOptP = state.getPointsToEPS(eps.e)
                val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numElements else 0
                val typesLeft = state.typesForCallSite(callSite)
                for (newType ← ub.dropOldest(seenTypes)) {
                    if (typesLeft.contains(newType.id)) {
                        state.removeTypeForCallSite(callSite, newType)
                        val (pc, name, descriptor, declaredType) = callSite
                        val tgtR = project.instanceCall(
                            state.method.declaringClassType.asObjectType,
                            newType,
                            name,
                            descriptor
                        )
                        handleCall(
                            state.method, name, descriptor, declaredType, pc, tgtR, calls
                        )
                    }

                }
            }

            if (isFinal) {
                state.removePointsToDependency(e)
            } else {
                state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsTo]])
            }
            returnResult(calls)(state)

        case _ ⇒ super.c(state)(eps)
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): PointsToBasedCGState = new PointsToBasedCGState(definedMethod, tacEP)

}

object PointsToBasedCallGraphScheduler extends CallGraphAnalysisScheduler {

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.ub(PointsTo)

    override def initializeAnalysis(p: SomeProject): CallGraphAnalysis = {
        new PointsToBasedCallGraph(p)
    }
}