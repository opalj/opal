/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.si.Project

/**
 * @author Michael Eichberg
 */
trait FPCFLazyLikeAnalysisScheduler[P <: Project] extends FPCFAnalysisScheduler[P] {

    override def derivesLazily: Some[PropertyBounds]

    override final def derivesEagerly: Set[PropertyBounds] = Set.empty

    override final def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override final def schedule(
        ps: PropertyStore,
        i:  InitializationData
    ): FPCFAnalysis = {
        register(ps.context(classOf[Project]).asInstanceOf[P], ps, i)
    }

    final def register(project: P, i: InitializationData): FPCFAnalysis = {
        register(project, project.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using
     * [[org.opalj.fpcf.PropertyStore.registerLazyPropertyComputation]] or
     * [[org.opalj.fpcf.PropertyStore.registerTransformer]] method.
     *
     * This method is typically called by the [[org.opalj.br.fpcf.FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerTriggeredComputation` or a variant of
     *       `scheduleEagerComputationForEntity`.
     */
    def register(
        project:       P,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): FPCFAnalysis

}
