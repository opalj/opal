/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.alias.Alias
import org.opalj.br.fpcf.properties.alias.AliasEntity
import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.alias.AliasUVar
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.TACAI

class IntraProceduralAliasAnalysis(final val project: SomeProject) extends AllocationSiteAndTacBasedAliasAnalysis {

    override type AnalysisState = TacBasedAliasAnalysisState with AllocationSiteBasedAliasAnalysisState
    override type AnalysisContext = AliasAnalysisContext

    protected[this] def analyzeTAC()(
        implicit
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
     * It is responsible for calculating the points-to set of the given [[AliasSourceElement]] and handling it
     * by updating the analysis state accordingly.
     */
    private[this] def handleElement(ase: AliasSourceElement, tac: Option[Tac])(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {
        /*ase match {
            case uVar: AliasUVar => handleUVar(uVar, tac.get)
            case other: _ => handleOther(other)
        }*/

        if (ase.isAliasUVar) handleUVar(ase.asAliasUVar, tac.get)
        else handleOther(ase)
    }

    /**
     * Handles the given [[AliasUVar]].
     *
     * It is responsible for calculating the points-to set of the given [[AliasUVar]] and handling it
     * by updating the analysis state accordingly. It assumes that it can point to any allocation site
     * if the variable interacts with constructs outside the current method.
     */
    private[this] def handleUVar(uVar: AliasUVar, tac: Tac)(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {

        uVar.persistentUVar.defSites.foreach(pc => {

            if (pc < 0) {
                state.setPointsToAny(uVar)
                return
            }

            tac.stmts(tac.pcToIndex(pc)) match {
                case Assignment(_, _, New(pc, _)) =>
                    state.addPointsTo(uVar, context.contextOf(uVar), pc)
                case _ =>
                    state.setPointsToAny(uVar)
                    return
            }

        })

    }

    /**
     * Handles the given [[AliasSourceElement]].
     *
     * Because we are only performing an intraprocedural analysis, we cannot handle it further and simply set,
     * that the element can point to any arbitrary object.
     */
    private[this] def handleOther(ase: AliasSourceElement)(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Unit = {
        state.setPointsToAny(ase)
    }

    override protected[this] def createState: AnalysisState =
        new AllocationSiteBasedAliasAnalysisState with TacBasedAliasAnalysisState

    override protected[this] def createContext(
        entity: AliasEntity
    ): AliasAnalysisContext =
        new AliasAnalysisContext(entity, project, propertyStore)
}

sealed trait IntraProceduralAliasAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq()

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(Alias)

    override def uses: Set[PropertyBounds] = Set(PropertyBounds.ub(TACAI))

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

/**
 * A scheduler for a lazy, intraprocedural alias analysis.
 */
object LazyIntraProceduralAliasAnalysisScheduler extends IntraProceduralAliasAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             LazyIntraProceduralAliasAnalysisScheduler.InitializationData
    ): FPCFAnalysis = {

        val analysis = new IntraProceduralAliasAnalysis(project)

        propertyStore.registerLazyPropertyComputation(
            Alias.key,
            analysis.determineAlias
        )

        analysis
    }
}
