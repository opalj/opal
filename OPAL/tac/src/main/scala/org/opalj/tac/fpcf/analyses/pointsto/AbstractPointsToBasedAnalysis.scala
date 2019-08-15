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
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey

// TODO remove this
trait AbstractPointsToBasedAnalysis extends FPCFAnalysis {

    type ElementType >: Null
    type PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]
    type State <: TACAIBasedAnalysisState
    type DependerType

    protected[this] implicit val definitionSites: DefinitionSites = {
        p.get(DefinitionSitesKey)
    }
    implicit val formalParameters: VirtualFormalParameters = {
        p.get(VirtualFormalParametersKey)
    }

    protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet]
    protected[this] def emptyPointsToSet: PointsToSet

    protected[this] def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): PointsToSet

    @inline protected[this] def currentPointsTo(
        depender: DependerType,
        dependee: Entity
    )(implicit state: State): PointsToSet

    @inline protected[this] def currentPointsToOfDefSites(
        depender: DependerType,
        defSites: IntTrieSet
    )(implicit state: State): Iterator[PointsToSet] = {
        defSites.iterator.map[PointsToSet](currentPointsToDefSite(depender, _))
    }

    @inline protected[this] def currentPointsToDefSite(
        depender: DependerType, dependeeDefSite: Int
    )(implicit state: State): PointsToSet = {
        if (ai.isMethodExternalExceptionOrigin(dependeeDefSite)) {
            val pc = ai.pcOfMethodExternalException(dependeeDefSite)
            val defSite = toEntity(pc, state.method, state.tac.stmts).asInstanceOf[DefinitionSite]
            currentPointsTo(depender, CallExceptions(defSite))
        } else if (ai.isImmediateVMException(dependeeDefSite)) {
            // FIXME -  we need to get the actual exception type here
            createPointsToSet(
                ai.pcOfImmediateVMException(dependeeDefSite),
                state.method,
                ObjectType.Throwable,
                isConstant = false
            )
        } else {
            currentPointsTo(depender, toEntity(dependeeDefSite, state.method, state.tac.stmts))
        }
    }

    @inline protected[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }
}
