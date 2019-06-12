/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyKey
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey

/**
 * Defines a trait with convenience methods often required by points-to based analyses.
 *
 * @author Florian Kuebler
 */
trait AbstractPointsToBasedAnalysis[Depender, PointsToSet <: PointsToSetLike[_, _, _]] extends FPCFAnalysis {

    type State <: AbstractPointsToState[Depender, PointsToSet]

    protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet]
    protected[this] def emptyPointsToSet: PointsToSet

    protected[this] val formalParameters: VirtualFormalParameters =
        p.get(VirtualFormalParametersKey)

    protected[this] val definitionSites: DefinitionSites = p.get(DefinitionSitesKey)

    @inline protected[this] def toEntity(
        defSite: Int
    )(implicit state: TACAIBasedAnalysisState): Entity = {
        if (defSite < 0) {
            formalParameters.apply(state.method)(-1 - defSite)
        } else {
            definitionSites(state.method.definedMethod, state.tac.stmts(defSite).pc)
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Depender, dependeeDefSite: Int
    )(implicit state: State): PointsToSet = {
        if (ai.isMethodExternalExceptionOrigin(dependeeDefSite)) {
            // FIXME ask what exception has been thrown
            emptyPointsToSet
        } else if (ai.isImmediateVMException(dependeeDefSite)) {
            // FIXME -  we need to get the actual exception type here
            emptyPointsToSet
        } else {
            currentPointsTo(depender, toEntity(dependeeDefSite))
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Depender, dependee: Entity
    )(implicit state: State): PointsToSet = {
        if (state.hasLocalPointsToSet(dependee)) {
            state.localPointsToSet(dependee)
        } else if (state.hasPointsToDependee(dependee)) {
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

    @inline protected[this] def currentPointsTo(
        depender: Depender,
        defSites: IntTrieSet
    )(
        implicit
        state: State
    ): Iterator[PointsToSet] = {
        defSites.iterator.map[PointsToSet](currentPointsTo(depender, _))
    }

    @inline private[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }
}
