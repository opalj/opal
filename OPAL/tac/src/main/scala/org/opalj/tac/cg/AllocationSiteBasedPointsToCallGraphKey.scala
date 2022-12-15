/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.br.fpcf.JavaFPCFAnalysisScheduler
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.AllocationSitesPointsToTypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedLibraryPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedNewInstanceAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedUnsafePointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.ReflectionAllocationsAnalysisScheduler

/**
 * A [[JavaProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object AllocationSiteBasedPointsToCallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey, SimpleContextsKey) ++:
            super.requirements(project)
    }

    override protected[cg] def callGraphSchedulers(
        project: SomeProject
    ): Iterable[JavaFPCFAnalysisScheduler] = {
        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"
        List(
            AllocationSiteBasedPointsToAnalysisScheduler,
            AllocationSiteBasedConfiguredMethodsPointsToAnalysisScheduler,
            AllocationSiteBasedTamiFlexPointsToAnalysisScheduler,
            AllocationSiteBasedArraycopyPointsToAnalysisScheduler,
            AllocationSiteBasedUnsafePointsToAnalysisScheduler,
            ReflectionAllocationsAnalysisScheduler,
            AllocationSiteBasedNewInstanceAnalysisScheduler
        ) ::: (if (isLibrary) List(AllocationSiteBasedLibraryPointsToAnalysisScheduler) else Nil)
    }

    override def getTypeIterator(project: SomeProject) =
        new AllocationSitesPointsToTypeIterator(project)
}
