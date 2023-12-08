/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyKey
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSites
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.ContextualAnalysis
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

/**
 * Provides methods in order to work with points-to sets.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait AbstractPointsToBasedAnalysis extends FPCFAnalysis with ContextualAnalysis {

    protected[this] type ElementType
    protected[this] type PointsToSet >: Null <: PointsToSetLike[ElementType, _, PointsToSet]
    protected[this] type State <: TACAIBasedAnalysisState[ContextType]
    protected[this] type DependerType

    implicit protected[this] val typeIterator: TypeIterator

    implicit protected[this] val definitionSites: DefinitionSites          = p.get(DefinitionSitesKey)
    implicit protected[this] val formalParameters: VirtualFormalParameters = p.get(VirtualFormalParametersKey)
    implicit protected[this] val declaredFields: DeclaredFields            = p.get(DeclaredFieldsKey)

    protected[this] val pointsToPropertyKey: PropertyKey[PointsToSet]
    protected[this] def emptyPointsToSet: PointsToSet

    protected[this] def createPointsToSet(
        pc: Int,
        callContext: ContextType,
        allocatedType: ReferenceType,
        isConstant: Boolean,
        isEmptyArray: Boolean = false): PointsToSet

    @inline protected[this] def getTypeOf(element: ElementType): ReferenceType

    @inline protected[this] def getTypeIdOf(element: ElementType): Int

    @inline protected[this] def isEmptyArray(element: ElementType): Boolean

    @inline protected[this] def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet =
        if (eOptP.hasUBP) eOptP.ub
        else emptyPointsToSet
}

object AbstractPointsToBasedAnalysis {
    val requiredProjectInformation: ProjectInformationKeys =
        Seq(TypeIteratorKey, DefinitionSitesKey, VirtualFormalParametersKey, DeclaredFieldsKey)
}
