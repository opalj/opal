/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedScalaCallGraphAnalysis
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DoPrivilegedPointsToScalaCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedArraycopyPointsToScalaAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToScalaAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedTamiFlexPointsToAnalysisScalaScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedUnsafePointsToScalaAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.ConfiguredMethodsPointsToScalaAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToScalaCallGraphKey extends AbstractCallGraphKey {
    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            AllocationSiteBasedPointsToBasedScalaCallGraphAnalysis,
            AllocationSiteBasedPointsToScalaAnalysisScheduler,
            ConfiguredMethodsPointsToScalaAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScalaScheduler,
            DoPrivilegedPointsToScalaCGAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToScalaAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToScalaAnalysisScheduler,
            AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysisScheduler
        )
    }
}
