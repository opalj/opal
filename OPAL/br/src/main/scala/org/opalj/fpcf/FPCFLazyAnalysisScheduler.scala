/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.SomeProject

/**
 *  The underlying analysis will only be registered with the property store and
 *  scheduled for a specific entity if queried.
 *
 * @author Michael Eichberg
 */
trait FPCFLazyAnalysisScheduler extends AbstractFPCFAnalysisScheduler {

    final override def computationType: ComputationType = LazyComputation

    override def derivesLazily: Some[PropertyBounds]

    final override def derivesEagerly: Set[PropertyBounds] = Set.empty

    final override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def schedule(ps: PropertyStore, i: InitializationData): Unit = {
        startLazily(ps.context(classOf[SomeProject]), ps, i)
    }

    final def register(project: SomeProject, i: InitializationData): FPCFAnalysis = {
        startLazily(project, project.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using [[PropertyStore.registerLazyPropertyComputation]].
     * This method is typically called by the [[org.opalj.fpcf.FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerTriggeredComputation` or a variant of
     *       `scheduleEagerComputationForEntity`.
     */
    // TODO Rename to "register"
    def startLazily(
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): FPCFAnalysis

}

/**
 * A simple eager analysis scheduler for those analyses that do not perform special initialization
 * steps.
 */
// TODO Rename => Simple...
trait BasicFPCFLazyAnalysisScheduler extends FPCFLazyAnalysisScheduler {

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null
    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}
    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
