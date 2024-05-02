/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias
package pointsto

import scala.collection.mutable.ArrayBuffer

import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasEntity
import org.opalj.br.fpcf.properties.alias.AliasField
import org.opalj.br.fpcf.properties.alias.AliasFormalParameter
import org.opalj.br.fpcf.properties.alias.AliasReturnValue
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.alias.AliasStaticField
import org.opalj.br.fpcf.properties.alias.AliasUVar
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.alias.TacBasedAliasAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToBasedAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.toEntity
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A base trait for all alias analyses based on the points-to information.
 */
trait AbstractPointsToBasedAliasAnalysis extends TacBasedAliasAnalysis with AbstractPointsToBasedAnalysis
    with SetBasedAliasAnalysis {

    override protected[this] type AnalysisContext = AliasAnalysisContext
    override protected[this] type AnalysisState <: PointsToBasedAliasAnalysisState[_, AliasSet]

    override protected[this] def analyzeTAC()(implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {

        handleElement(context.element1, state.tacai1)
        handleElement(context.element2, state.tacai2)

        createResult()
    }

    /**
     * Handles the given [[AliasSourceElement]].
     *
     * It is responsible for retrieving current the points-to set of the given [[AliasSourceElement]] and handling it
     * by updating the analysis state accordingly.
     */
    private[this] def handleElement(ase: AliasSourceElement, tac: Option[Tac])(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {

        ase match {
            case AliasUVar(uVar, _, _) =>
                uVar.defSites.foreach(ds => {
                    handlePointsToEntity(ase, getPointsToOfDefSite(ds, context.contextOf(ase), tac.get))
                })

            case AliasFormalParameter(fp) =>
                handlePointsToEntity(ase, getPointsToOfDefSite(fp.origin, context.contextOf(ase), tac.get))

            case AliasStaticField(field) => handlePointsToEntity(ase, getPointsToOfStaticField(field))

            case field: AliasField =>
                handlePointsToEntities(ase, getPointsToOfField(field, context.contextOf(ase), tac.get))

            case arv: AliasReturnValue => handlePointsToEntity(arv, getPointsToOfReturnValue(context.contextOf(arv)))

            case _ =>
        }
    }

    /**
     * Retrieves the points-to set of the given definition site.
     */
    private[this] def getPointsToOfDefSite(defSite: Int, context: Context, tac: Tac): EOptionP[Entity, PointsToSet] = {
        propertyStore(
            toEntity(if (defSite < 0) defSite else tac.properStmtIndexForPC(defSite), context, tac.stmts),
            pointsToPropertyKey
        )
    }

    /**
     * Retrieves the points-to set of the given static field.
     */
    private[this] def getPointsToOfStaticField(field: Field): EOptionP[Entity, PointsToSet] = {
        propertyStore(
            declaredFields.apply(field),
            pointsToPropertyKey
        )
    }

    /**
     * Retrieves the points-to set of the given non-static field.
     * If the points-to set of one of the defSites of the fieldReference is refineable, it is added as a field dependency.
     */
    private[this] def getPointsToOfField(field: AliasField, fieldContext: Context, tac: Tac)(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Iterable[EOptionP[Entity, PointsToSet]] = {

        val allocationSites = ArrayBuffer.empty[ElementType]

        field.fieldReference.defSites.map(getPointsToOfDefSite(_, fieldContext, tac))
            .foreach(pts => {

                if (pts.isEPK) {
                    state.addDependency(pts)
                    state.addFieldDependency(field, pts)
                } else {

                    val fieldReferenceEntity = (field.fieldReference, pts.e)
                    pts.ub.forNewestNElements(pts.ub.numElements - state.pointsToElementsHandled(
                        field,
                        fieldReferenceEntity
                    )) {
                        value =>
                            {
                                allocationSites += value
                                state.incPointsToElementsHandled(field, fieldReferenceEntity)
                            }
                    }
                }
            })

        allocationSites.map(allocSite =>
            propertyStore((allocSite, declaredFields(field.fieldReference.field)), pointsToPropertyKey)
        )
    }

    /**
     * Retrieves the points-to set of the return value of the given method.
     */
    private[this] def getPointsToOfReturnValue(callContext: Context): EOptionP[Entity, PointsToSet] = {
        propertyStore(
            callContext,
            pointsToPropertyKey
        )
    }

    /**
     * Handles all given points-to entities associated with given [[AliasSourceElement]] by updating the analysis state.
     */
    private[this] def handlePointsToEntities(ase: AliasSourceElement, eps: Iterable[EOptionP[Entity, PointsToSet]])(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {
        eps.foreach(handlePointsToEntity(ase, _))
    }

    /**
     * Handles the given points-to entity associated with the given [[AliasSourceElement]] by updating the analysis state.
     */
    private[this] def handlePointsToEntity(ase: AliasSourceElement, eps: EOptionP[Entity, PointsToSet])(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {

        val pointsToEntity: Entity = eps.e

        if (eps.isEPK) {
            state.addDependency(eps)
            state.addElementDependency(ase, eps)
        } else handlePointsToSet(ase, pointsToEntity, eps.ub)
    }

    /**
     * Handles the given points-to set associated with the given [[AliasSourceElement]] by updating the analysis state.
     *
     * @param ase The [[AliasSourceElement]] the given pointsTo set is associated with.
     * @param pointsToEntity The pointsTo entity the pointTo Set belongs to.
     * @param pointsToSet The pointsTo set to handle.
     */
    private[this] def handlePointsToSet(ase: AliasSourceElement, pointsToEntity: Entity, pointsToSet: PointsToSet)(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {
        pointsToSet.forNewestNElements(pointsToSet.numElements - state.pointsToElementsHandled(ase, pointsToEntity)) {
            element => handlePointsToSetElement(ase, pointsToEntity, element)
        }
    }

    /**
     * Handles a single element of the given points-to set that is associated with [[AliasSourceElement]] and has not been handled so far.
     *
     * @param ase The [[AliasSourceElement]] the pointsTo set is associated with.
     * @param pointsToEntity The pointsTo entity the pointTo Set belongs to.
     * @param element The pointsTo Element to handle.
     */
    protected[this] def handlePointsToSetElement(ase: AliasSourceElement, pointsToEntity: Entity, element: ElementType)(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit

    /**
     * Continues the computation when any depending property is updated.
     */
    override protected[this] def continuation(someEPS: SomeEPS)(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {

        someEPS match {
            case UBP(pointsToSet: PointsToSetLike[_, _, _]) =>
                val pts = pointsToSet.asInstanceOf[PointsToSet]

                val field1Dependence = state.field1HasDependency(someEPS)
                val field2Dependence = state.field2HasDependency(someEPS)

                if (field1Dependence) handlePointsToSet(
                    context.element1,
                    (context.element1.asAliasField.fieldReference, someEPS.e),
                    pts
                )
                if (field2Dependence) handlePointsToSet(
                    context.element2,
                    (context.element2.asAliasField.fieldReference, someEPS.e),
                    pts
                )

                state.removeFieldDependency(someEPS)

                if (someEPS.isRefinable) {
                    if (field1Dependence) state.addField1Dependency(someEPS)
                    if (field2Dependence) state.addField2Dependency(someEPS)

                }

                val element1Dependence = state.element1HasDependency(someEPS)
                val element2Dependence = state.element2HasDependency(someEPS)

                if (element1Dependence) handlePointsToSet(context.element1, someEPS.e, pts)
                if (element2Dependence) handlePointsToSet(context.element2, someEPS.e, pts)

                state.removeElementDependency(someEPS)

                if (someEPS.isRefinable) {
                    if (element1Dependence) state.addElementDependency(context.element1, someEPS)
                    if (element2Dependence) state.addElementDependency(context.element2, someEPS)
                }

                createResult()
            case _ => super.continuation(someEPS)
        }
    }

    override protected[this] def createContext(
        entity: AliasEntity
    ): AnalysisContext =
        new AliasAnalysisContext(entity, project, propertyStore)

}

/**
 * A base trait for all points-to based alias analysis schedulers.
 */
trait PointsToBasedAliasAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(
            VirtualFormalParametersKey,
            TypeIteratorKey,
            DefinitionSitesKey,
            DeclaredFieldsKey
        )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(Alias)

    override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(Callees),
        PropertyBounds.ub(AllocationSitePointsToSet)
    )

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}
