/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Uses the [[PointsToSetLike]] of
 * [[org.opalj.tac.common.DefinitionSite]] and[[org.opalj.br.analyses.VirtualFormalParameter]]s
 * in order to determine the targets of virtual method calls.
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToBasedCallGraphAnalysis[PointsToSet <: PointsToSetLike[_, _, PointsToSet]]
    extends AbstractCallGraphAnalysis
    with AbstractPointsToBasedAnalysis {

    override type State = PointsToBasedCGState[PointsToSet]
    override type DependerType = CallSite
    override type LocalTypeInformation = PointsToSet

    override def c(
        state: State
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case EUBPS(e, ub: PointsToSetLike[_, _, _], isFinal) ⇒
                val relevantCallSites = state.dependersOf(e)

                // ensures, that we only add new calls
                val calls = new DirectCalls()

                val oldEOptP: EOptionP[Entity, PointsToSet] = state.getPointsToProperty(eps.e)
                val seenTypes = if (oldEOptP.hasUBP) oldEOptP.ub.numTypes else 0

                // perform the update for the new types
                for (callSite ← relevantCallSites) {
                    val typesLeft = state.typesForCallSite(callSite)
                    ub.forNewestNTypes(ub.numTypes - seenTypes) { newType ⇒
                        val theType =
                            if (newType.isObjectType) newType.asObjectType else ObjectType.Object
                        if (typesLeft.contains(theType.id)) {
                            state.removeTypeForCallSite(callSite, theType)
                            val CallSite(pc, name, descriptor, declaredType) = callSite
                            val tgtR = project.instanceCall(
                                state.method.declaringClassType,
                                theType,
                                name,
                                descriptor
                            )
                            handleCall(
                                state.method, name, descriptor, declaredType, pc, tgtR, calls
                            )
                        }
                    }
                }

                // The method removeTypesForCallSite might have made the dependency obsolete, so only
                // update or remove it, if we still need updates for that type.
                if (state.hasPointsToDependee(e)) {
                    if (isFinal) {
                        state.removePointsToDependee(e)
                    } else {
                        state.updatePointsToDependency(eps.asInstanceOf[EPS[Entity, PointsToSet]])
                    }
                }

                returnResult(calls)(state)

            case _ ⇒
                super.c(state)(eps)
        }
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): PointsToBasedCGState[PointsToSet] = {
        new PointsToBasedCGState[PointsToSet](definedMethod, tacEP)
    }

    @inline override protected[this] def currentPointsTo(
        depender:   DependerType,
        dependee:   Entity,
        typeFilter: ReferenceType ⇒ Boolean = PointsToSetLike.noFilter
    )(implicit state: State): PointsToSet = {
        if (state.hasPointsToDependee(dependee)) {
            val p2s = state.getPointsToProperty(dependee)

            // It might be the case that there a dependency for that points-to state in the state
            // from another depender.
            if (!state.hasPointsToDependency(depender, dependee)) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
        } else {
            val p2s = propertyStore(dependee, pointsToPropertyKey)
            if (p2s.isRefinable) {
                state.addPointsToDependency(depender, p2s)
            }
            pointsToUB(p2s)
        }
    }

    @inline override protected[this] def canResolveCall(
        localTypeInformation: LocalTypeInformation,
        state:                State
    ): ObjectType ⇒ Boolean = { newType ⇒
        (localTypeInformation.types.contains(newType) ||
            (newType eq ObjectType.Object) && localTypeInformation.types.exists(_.isArrayType))
    }

    @inline protected[this] def handleUnresolvedCall(
        unresolvedTypes: IntTrieSet,
        callSite:        CallSite
    )(implicit state: State): Unit = {
        state.addPotentialTypesOfCallSite(callSite, unresolvedTypes)
    }

    @inline protected[this] def getLocalTypeInformation(
        callSite: CallSite, call: Call[V] with VirtualCall[V]
    )(implicit state: State): LocalTypeInformation = {
        // get the upper bound of the pointsToSet and creates a dependency if needed
        val currentPointsToSets = currentPointsToOfDefSites(callSite, call.receiver.asVar.definedBy)
        currentPointsToSets.foldLeft(emptyPointsToSet) { (r, l) ⇒ r.included(l) }
    }

}

class TypeBasedPointsToBasedCallGraphAnalysis private[pointsto] (
        final val project: SomeProject
) extends AbstractPointsToBasedCallGraphAnalysis[TypeBasedPointsToSet] with TypeBasedAnalysis

object TypeBasedPointsToBasedCallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.ub(TypeBasedPointsToSet)

    override def initializeAnalysis(p: SomeProject): TypeBasedPointsToBasedCallGraphAnalysis = {
        new TypeBasedPointsToBasedCallGraphAnalysis(p)
    }
}

class AllocationSiteBasedPointsToBasedCallGraphAnalysis private[pointsto] (
        final val project: SomeProject
) extends AbstractPointsToBasedCallGraphAnalysis[AllocationSitePointsToSet]
    with AllocationSiteBasedAnalysis

object AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {

    override def uses: Set[PropertyBounds] = {
        super.uses + PropertyBounds.ub(AllocationSitePointsToSet)
    }

    override def initializeAnalysis(
        p: SomeProject
    ): AllocationSiteBasedPointsToBasedCallGraphAnalysis = {
        new AllocationSiteBasedPointsToBasedCallGraphAnalysis(p)
    }
}