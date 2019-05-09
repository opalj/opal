/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.fpcf.ComputationType
import org.opalj.fpcf.LazyComputation
import org.opalj.fpcf.PropertyStore
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
    override def init(p: SomeProject, ps: PropertyStore): Null = null
    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}
}
