/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.scheduling

import org.opalj.fpcf.{ComputationType, LazyComputation, PropertyStore}
import org.opalj.si.{FPCFAnalysis, MetaProject}

/**
 *  The underlying analysis will only be registered with the property store and
 *  scheduled for a specific entity if queried.
 *
 * @author Michael Eichberg
 */
trait FPCFLazyAnalysisScheduler[P <: MetaProject] extends FPCFLazyLikeAnalysisScheduler[P] {

    final override def computationType: ComputationType = LazyComputation

}

/**
 * A simple eager analysis scheduler for those analyses that do not perform special initialization
 * steps.
 */
// TODO Rename => Simple...
trait BasicFPCFLazyAnalysisScheduler[P <: MetaProject] extends FPCFLazyAnalysisScheduler[P] {
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
