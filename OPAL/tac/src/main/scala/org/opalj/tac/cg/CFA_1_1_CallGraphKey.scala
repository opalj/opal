/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.scheduling.FPCFAnalysisScheduler
import org.opalj.si.ProjectInformationKey
import org.opalj.tac.fpcf.analyses.cg.CFA_k_l_TypeIterator

/**
 * A [[ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_1_CallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        AllocationSiteBasedPointsToCallGraphKey.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        AllocationSiteBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }
    override def getTypeIterator(project: SomeProject) =
        new CFA_k_l_TypeIterator(project, 1, 1)
}
