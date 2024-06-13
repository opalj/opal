/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias
package pointsto

import org.opalj.br.PC
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.pointsto.longToAllocationSite
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.alias.AllocationSite
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis

/**
 * An alias analysis based on points-to information that contain the possible allocation sites of an element.
 * @param project The project
 */
class AllocationSitePointsToBasedAliasAnalysis(final val project: SomeProject)
    extends AbstractPointsToBasedAliasAnalysis
    with AbstractPointsToAnalysis
    with AllocationSiteBasedAnalysis
    with AllocationSiteAndTacBasedAliasAnalysis {

    override protected[this] type AnalysisState = AllocationSitePointsToBasedAliasAnalysisState

    override protected[this] def handlePointsToSetElement(
        ase:            AliasSourceElement,
        pointsToEntity: Entity,
        element:        ElementType
    )(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {

        val encodedAllocationSite: ElementType = element

        val (allocContext, pc, _): (Context, PC, Int) = longToAllocationSite(encodedAllocationSite)

        state.addPointsTo(ase, allocContext, pc)
        state.incPointsToElementsHandled(ase, pointsToEntity)
    }

    /**
     * Creates the state to use for the computation.
     */
    override protected[this] def createState: AnalysisState = new AllocationSitePointsToBasedAliasAnalysisState

}

/**
 * A scheduler for a lazy, allocation site, points-to based alias analysis.
 */
object LazyAllocationSitePointsToBasedAliasAnalysisScheduler extends PointsToBasedAliasAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             LazyAllocationSitePointsToBasedAliasAnalysisScheduler.InitializationData
    ): FPCFAnalysis = {

        val analysis = new AllocationSitePointsToBasedAliasAnalysis(project)

        propertyStore.registerLazyPropertyComputation(
            Alias.key,
            analysis.determineAlias
        )

        analysis
    }
}

/**
 * The state class used by an [[AllocationSitePointsToBasedAliasAnalysis]].
 *
 * @see [[PointsToBasedAliasAnalysisState]]
 */
class AllocationSitePointsToBasedAliasAnalysisState extends AllocationSiteBasedAliasAnalysisState
    with PointsToBasedAliasAnalysisState[AllocationSite, AllocationSiteBasedAliasSet] {}