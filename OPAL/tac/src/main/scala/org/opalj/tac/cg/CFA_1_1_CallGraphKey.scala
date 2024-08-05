/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.CallStringContexts
import org.opalj.br.fpcf.properties.CallStringContextsKey
import org.opalj.tac.fpcf.analyses.cg.CFA_k_l_TypeIterator

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_1_CallGraphKey extends PointsToCallGraphKey {

    override val pointsToType: String = "AllocationSiteBased"
    override val contextKey: ProjectInformationKey[CallStringContexts, Nothing] = CallStringContextsKey

    override protected[cg] def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        AllocationSiteBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }

    override def getTypeIterator(project: SomeProject) =
        new CFA_k_l_TypeIterator(project, 1, 1)
}
