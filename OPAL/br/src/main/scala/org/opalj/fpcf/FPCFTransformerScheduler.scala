/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.br.analyses.SomeProject

/**
 *  The underlying analysis will only be registered with the property store and
 *  called by the store when a final property of kind `sourcePK` for an entity of type E
 *  is computed.
 *
 * @author Michael Eichberg
 */
trait FPCFTransformerScheduler extends AbstractFPCFAnalysisScheduler {

    final override def computationType: ComputationType = Transformer

    override def derivesLazily: Some[PropertyBounds]

    final override def derivesEagerly: Set[PropertyBounds] = Set.empty

    final override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def schedule(ps: PropertyStore, i: InitializationData): Unit = {
        register(ps.context(classOf[SomeProject]), ps, i)
    }

    final def register(project: SomeProject, i: InitializationData): Unit = {
        register(project, project.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using [[PropertyStore.registerTransformer]].
     * This method is typically called by the [[org.opalj.fpcf.FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerLazyPropertyComputation` or a variant of
     *       `scheduleEagerComputationForEntity`.
     */
    def register(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): Unit
}

trait BasicFPCFTransformerScheduler extends FPCFTransformerScheduler {
    final override type InitializationData = Null
    def init(p: SomeProject, ps: PropertyStore): Null = null
    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}
    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
