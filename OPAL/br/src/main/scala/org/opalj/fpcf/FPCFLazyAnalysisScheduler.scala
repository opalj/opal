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
trait FPCFLazyAnalysisScheduler extends FPCFLazyLikeAnalysisScheduler {

    final override def computationType: ComputationType = LazyComputation

}

/**
 * A simple eager analysis scheduler for those analyses that do not perform special initialization
 * steps.
 */
// TODO Rename => Simple...
trait BasicFPCFLazyAnalysisScheduler extends FPCFLazyAnalysisScheduler {
    override type InitializationData = Null
    def init(p: SomeProject, ps: PropertyStore): Null = null
    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}
    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
