/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.xta.SimpleInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTACallGraphAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on Tip and
 * Palsberg's XTA algorithm.
 *
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * TODO AB document more
 *
 * @author Andreas Bauer
 */
object XTACallGraphKey extends AbstractCallGraphKey {

    override protected def requirements: ProjectInformationKeys = {
        super.requirements :+ InitialInstantiatedTypesKey
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        // TODO AB configure this properly depending on library/application (see RTA key)
        List(
            SimpleInstantiatedTypesAnalysisScheduler,
            XTACallGraphAnalysisScheduler
        )
    }
}
