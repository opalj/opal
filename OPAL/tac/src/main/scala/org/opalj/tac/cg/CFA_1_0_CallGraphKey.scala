/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.CallStringContextsKey
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.cg.CFA_k_0_TypeProvider
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedUnsafePointsToAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author DominikHelm
 */
object CFA_1_0_CallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(DefinitionSitesKey, VirtualFormalParametersKey, CallStringContextsKey) ++:
            super.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            TypeBasedPointsToAnalysisScheduler,
            TypeBasedConfiguredMethodsPointsToAnalysisScheduler,
            TypeBasedTamiFlexPointsToAnalysisScheduler,
            TypeBasedArraycopyPointsToAnalysisScheduler,
            TypeBasedUnsafePointsToAnalysisScheduler
        )
    }

    override def getTypeProvider(project: SomeProject) = new CFA_k_0_TypeProvider(project, 1)

}
