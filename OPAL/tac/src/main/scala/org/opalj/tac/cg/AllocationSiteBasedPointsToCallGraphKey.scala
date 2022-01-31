/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeProvider
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedUnsafePointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.ReflectionAllocationsAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToCallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey) ++: super.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToAnalysisScheduler,
            ReflectionAllocationsAnalysisScheduler
        )
    }
    override def getTypeProvider(project: SomeProject) =
        new AllocationSitesPointsToTypeProvider(project)
}
