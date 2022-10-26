/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.scheduling

import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.si.{FPCFAnalysis, MetaProject, PropertyStoreKey}

import scala.reflect.classTag

/**
 * @author Michael Eichberg
 */
trait FPCFLazyLikeAnalysisScheduler[P <: MetaProject] extends FPCFAnalysisScheduler[P] {

    override def derivesLazily: Some[PropertyBounds]

    final override def derivesEagerly: Set[PropertyBounds] = Set.empty

    final override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def schedule(
        ps: PropertyStore,
        i:  InitializationData
    ): FPCFAnalysis = {
        register(ps.context(classTag[P].runtimeClass).asInstanceOf[P], ps, i)
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
     * This method is typically called by the [[org.opalj.fpcf.scheduling.FPCFAnalysesManager]].
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
