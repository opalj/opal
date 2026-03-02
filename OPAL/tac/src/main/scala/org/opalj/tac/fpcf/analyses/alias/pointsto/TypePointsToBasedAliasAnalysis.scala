/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias
package pointsto

import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.pointsto.TypeBasedPointsToSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.analyses.alias.TacBasedAliasAnalysis
import org.opalj.tac.fpcf.analyses.alias.TypeBasedAliasAnalysis
import org.opalj.tac.fpcf.analyses.alias.TypeBasedAliasAnalysisState
import org.opalj.tac.fpcf.analyses.alias.TypeBasedAliasSet
import org.opalj.tac.fpcf.analyses.pointsto.AbstractPointsToAnalysis
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedAnalysis

/**
 * An alias analysis based on points-to information that contain the possible [[ReferenceType]]s of an element.
 * @param project The project
 */
class TypePointsToBasedAliasAnalysis(final val project: SomeProject)
    extends AbstractPointsToBasedAliasAnalysis
    with AbstractPointsToAnalysis
    with TypeBasedAnalysis
    with TypeBasedAliasAnalysis
    with TacBasedAliasAnalysis {

    override protected type AnalysisState = TypePointsToBasedAliasAnalysisState

    override protected def handlePointsToSetElement(
        ase:            AliasSourceElement,
        pointsToEntity: Entity,
        element:        ReferenceType
    )(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {
        state.addPointsTo(ase, element)
    }
    /**
     * Creates the state to use for the computation.
     */
    override protected def createState: AnalysisState = new TypePointsToBasedAliasAnalysisState
}

/**
 * A scheduler for a lazy, type, points-to based alias analysis.
 */
object LazyTypePointsToBasedAliasAnalysisScheduler extends PointsToBasedAliasAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {
    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             LazyTypePointsToBasedAliasAnalysisScheduler.InitializationData
    ): FPCFAnalysis = {

        val analysis = new TypePointsToBasedAliasAnalysis(project)

        propertyStore.registerLazyPropertyComputation(
            Alias.key,
            analysis.determineAlias
        )

        analysis
    }
}

/**
 * The state class used by an [[TypePointsToBasedAliasAnalysis]].
 *
 * @see [[PointsToBasedAliasAnalysisState]]
 */
class TypePointsToBasedAliasAnalysisState extends TypeBasedAliasAnalysisState
    with PointsToBasedAliasAnalysisState[ReferenceType, TypeBasedAliasSet, TypeBasedPointsToSet] {}
