/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject

/**
 * @author Michael Eichberg
 */
trait FPCFLazyLikeAnalysisScheduler extends FPCFAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds]

    final override def derivesEagerly: Set[PropertyBounds] = Set.empty

    final override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def schedule(
        ps: PropertyStore,
        i:  InitializationData
    ): FPCFAnalysis = {
        register(ps.context(classOf[SomeProject]), ps, i)
    }

    final def register(project: SomeProject, i: InitializationData): FPCFAnalysis = {
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
        project:       SomeProject,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): FPCFAnalysis

}

