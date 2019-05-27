/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EPK
import org.opalj.fpcf.UBPS
import org.opalj.br.ObjectType
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.pointsto.properties.PointsTo
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey

/**
 * Defines a trait with convenience methods often required by points-to based analyses.
 *
 * @author Florian Kuebler
 */
trait PointsToBasedAnalysis extends FPCFAnalysis {

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
        depender: Entity, dependeeDefSite: Int
    )(implicit state: AbstractPointsToState): UIDSet[ObjectType] = {
        if (ai.isMethodExternalExceptionOrigin(dependeeDefSite)) {
            UIDSet(ObjectType.Exception) // todo ask what exception has been thrown
        } else if (ai.isImmediateVMException(dependeeDefSite)) {
            // todo -  we need to get the actual exception type here
            UIDSet(ObjectType.Exception)
        } else {
            currentPointsTo(depender, toEntity(dependeeDefSite))
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Entity, dependee: Entity
    )(implicit state: AbstractPointsToState): UIDSet[ObjectType] = {
        val pointsToSetEOptP = state.getOrRetrievePointsToEPS(dependee, ps)
        pointsToSetEOptP match {
            case UBPS(pointsTo: PointsTo, isFinal) ⇒
                if (!isFinal) state.addPointsToDependency(depender, pointsToSetEOptP)
                pointsTo.types

            case _: EPK[Entity, PointsTo] ⇒
                state.addPointsToDependency(depender, pointsToSetEOptP)
                UIDSet.empty
        }
    }

    @inline protected[this] def currentPointsTo(
        depender: Entity,
        defSites: IntTrieSet
    )(
        implicit
        state: AbstractPointsToState
    ): UIDSet[ObjectType] = {
        defSites.foldLeft(UIDSet.empty[ObjectType]) { (pointsToSet, defSite) ⇒
            pointsToSet ++ currentPointsTo(depender, defSite)
        }
    }
}
