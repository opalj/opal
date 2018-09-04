/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.SomeProject

/**
 * Factory for FPCF analyses which should be directly started/scheduled.
 *
 * @author Michael Eichberg
 */
trait FPCFEagerAnalysisScheduler extends AbstractFPCFAnalysisScheduler {

    final override def isLazy: Boolean = false

    final override def schedule(ps: PropertyStore, i: InitializationData): Unit = {
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
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[org.opalj.fpcf.FPCFAnalysesManager]].
     */
    def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis
}
