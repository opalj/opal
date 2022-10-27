/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.scheduling

import org.opalj.fpcf._
import org.opalj.si.{FPCFAnalysis, MetaProject, PropertyStoreKey}

import scala.reflect.classTag

/**
 *  The underlying analysis will only be registered with the property store and
 *  will be triggered if a property of the specified kind is derived for a specific entity.
 *  The analysis computing the property which triggers this analysis has to be another
 *  triggered analysis, an eager analysis or a transformer; in the latter case the
 *  transformer's source property must not depend (neither directly nor indirectly) on
 *  a lazy analysis.
 *
 * @author Michael Eichberg
 */
trait FPCFTriggeredAnalysisScheduler[P <: MetaProject] extends FPCFAnalysisScheduler[P] {

    final override def computationType: ComputationType = TriggeredComputation

    final override def derivesLazily: Option[PropertyBounds] = None

    final override def schedule(ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        register(ps.context(classTag[P].runtimeClass).asInstanceOf[P], ps, i)
    }

    /**
     * Specifies the kind of the properties that will trigger the analysis to be registered.
     */
    def triggeredBy: PropertyKind

    final def register(project: P, i: InitializationData): FPCFAnalysis = {
        register(project, project.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using [[org.opalj.fpcf.PropertyStore#registerTriggeredComputation]].
     * This method is typically called by the [[FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerLazyPropertyComputation` or a variant of
     *       `scheduleEagerComputationForEntity`.
     */
    def register(
        project:       P,
        propertyStore: PropertyStore,
        i:             InitializationData
    ): FPCFAnalysis

}

trait BasicFPCFTriggeredAnalysisScheduler[P <: MetaProject] extends FPCFTriggeredAnalysisScheduler[P] {
    override type InitializationData = Null
    override def init(p: P, ps: PropertyStore): Null = null
    override def beforeSchedule(p: P, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        P,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
