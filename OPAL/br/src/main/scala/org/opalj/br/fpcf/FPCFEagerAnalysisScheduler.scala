/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.fpcf.ComputationType
import org.opalj.fpcf.EagerComputation
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject

/**
 * Factory for FPCF analyses which should be directly started/scheduled.
 *
 * @author Michael Eichberg
 */
trait FPCFEagerAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def computationType: ComputationType = EagerComputation

    final override def derivesLazily: Option[PropertyBounds] = None

    final override def schedule(ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        start(ps.context(classOf[SomeProject]), ps, i)
    }

    /**
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[FPCFAnalysesManager]].
     */
    def start(p: SomeProject, i: InitializationData): FPCFAnalysis = {
        start(p, p.get(PropertyStoreKey), i)
    }

    /**
     * Called when a schedule is executed and when this analysis shall register itself
     * with the property store using
     * [[org.opalj.fpcf.PropertyStore.scheduleEagerComputationForEntity]] or a variant thereof.
     * This method is typically implicitly called by the [[org.opalj.br.fpcf.FPCFAnalysesManager]].
     *
     * @note This analysis must not call `registerTriggeredComputation` or
     *       `registerLazyPropertyComputation`.
     */
    def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis
}

/**
 * A simple eager analysis scheduler for those analyses that do not perform special initialization
 * steps.
 */
// TODO Rename => Simple...
trait BasicFPCFEagerAnalysisScheduler extends FPCFEagerAnalysisScheduler {
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
