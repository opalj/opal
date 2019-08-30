/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DoPrivilegedPointsToCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.reflection.TamiFlexCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedUnsafePointsToAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToCallGraphKey extends AbstractCallGraphKey {
    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler, // TODO make this one independent
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            TamiFlexCallGraphAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScheduler,
            DoPrivilegedPointsToCGAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToAnalysisScheduler,
            AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
        )
    }
}
