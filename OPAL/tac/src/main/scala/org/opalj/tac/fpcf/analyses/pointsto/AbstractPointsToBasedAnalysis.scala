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
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
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

    protected type ElementType
    protected type PointsToSet >: Null <: PointsToSetLike[ElementType, ?, PointsToSet]
    protected type State <: TACAIBasedAnalysisState[ContextType]
    protected type DependerType

    protected implicit val typeIterator: TypeIterator

    protected implicit val definitionSites: DefinitionSites = p.get(DefinitionSitesKey)
    protected implicit val formalParameters: VirtualFormalParameters = p.get(VirtualFormalParametersKey)
    protected implicit val declaredFields: DeclaredFields = p.get(DeclaredFieldsKey)

    protected val pointsToPropertyKey: PropertyKey[PointsToSet]
    protected def emptyPointsToSet: PointsToSet

    protected def createPointsToSet(
        pc:            Int,
        callContext:   ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean = false
    ): PointsToSet

    @inline protected def getTypeOf(element: ElementType): ReferenceType

    @inline protected def getTypeIdOf(element: ElementType): Int

    @inline protected def isEmptyArray(element: ElementType): Boolean

    @inline protected def pointsToUB(eOptP: EOptionP[Entity, PointsToSet]): PointsToSet = {
        if (eOptP.hasUBP)
            eOptP.ub
        else
            emptyPointsToSet
    }
}

trait PointsToBasedAnalysisScheduler extends FPCFAnalysisScheduler {
    def requiredProjectInformation: ProjectInformationKeys =
        Seq(TypeIteratorKey, DefinitionSitesKey, VirtualFormalParametersKey, DeclaredFieldsKey)

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] =
        super.uses(p, ps) ++ p.get(TypeIteratorKey).usedPropertyKinds
}
