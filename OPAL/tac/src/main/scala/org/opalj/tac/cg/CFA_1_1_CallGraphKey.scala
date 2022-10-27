/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.JavaFPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.CFA_k_l_TypeIterator

/**
 * A [[JavaProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_1_CallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = {
        AllocationSiteBasedPointsToCallGraphKey.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[JavaFPCFAnalysisScheduler] = {
        AllocationSiteBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }
    override def getTypeIterator(project: SomeProject) =
        new CFA_k_l_TypeIterator(project, 1, 1)
}
