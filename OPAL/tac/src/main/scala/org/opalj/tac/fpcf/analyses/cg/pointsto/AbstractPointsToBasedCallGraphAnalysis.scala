/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import org.opalj.collection.ForeachRefIterator
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
    override type DependerType = CallSiteT

    override protected[this] def handlePreciseCall(
        calleeType:        ObjectType,
        caller:            DefinedMethod,
        callerType:        ObjectType,
        call:              Call[V] with VirtualCall[V],
        pc:                Int,
        calleesAndCallers: DirectCalls
    )(implicit state: State): Unit = {
        handleImpreciseCall(calleeType, caller, call, pc, calleesAndCallers)
    }

    /**
     * Computes the calls of the given `method` including the known effect of the `call` and
     * the call sites associated ith this call (in order to process updates of instantiated types).
     * There can be multiple "call sites", in case the three-address code has computed multiple
     * type bounds for the receiver.
     */
    override protected[this] def doHandleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: State): Unit = {
        val callerType = caller.definedMethod.classFile.thisType
        val callSite = (pc, call.name, call.descriptor, call.declaringClass)

        // get the upper bound of the pointsToSet and creates a dependency if needed
        val currentPointsToSets = currentPointsToOfDefSites(callSite, call.receiver.asVar.definedBy)
        val pointsToSet = currentPointsToSets.foldLeft(emptyPointsToSet) { (r, l) ⇒ r.included(l) }

        var types = IntTrieSet.empty

        for (newType ← potentialTargets) {
            if (pointsToSet.types.contains(newType) ||
                (newType eq ObjectType.Object) && pointsToSet.types.exists(_.isArrayType)) {
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
        state.addPotentialTypesOfCallSite(callSite, types)
    }

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
                            val (pc, name, descriptor, declaredType) = callSite
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