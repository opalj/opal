/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DoPrivilegedPointsToCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.reflection.TamiFlexCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler

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
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        List(
            AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler, // TODO make this one independent
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            TamiFlexCallGraphAnalysisScheduler,
            DoPrivilegedPointsToCGAnalysisScheduler
        )
    }
}
