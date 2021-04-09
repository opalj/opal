/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DoPrivilegedPointsToCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
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

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey) ++: super.requirements(project)
    }

    // FIXME This is just a hack as long as modules are not compatible with the pointsto call graph
    override def registeredAnalyses(project: SomeProject): Seq[FPCFAnalysisScheduler] = Seq.empty

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            AllocationSiteBasedPointsToBasedCallGraphAnalysisScheduler, // TODO make this one independent
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScheduler,
            DoPrivilegedPointsToCGAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToAnalysisScheduler,
            AllocationSiteBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
        )
    }
}
