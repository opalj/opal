/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.ComputationSpecification
import org.opalj.tac.fpcf.analyses.cg.xta.CTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.FTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.MTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.PropagationBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.SetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.SimpleInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTASetEntitySelector

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on Tip and
 * Palsberg's propagation-based algorithms.
 *
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * TODO AB document more
 *
 * @author Andreas Bauer
 */
trait PropagationBasedCallGraphKey extends AbstractCallGraphKey {

    override protected def requirements: ProjectInformationKeys = {
        super.requirements :+ InitialInstantiatedTypesKey
    }

    def setEntitySelector(): SetEntitySelector

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        val theSetEntitySelector = setEntitySelector()

        // TODO AB Configure this properly depending on library/application (see: RTA key)
        List(
            PropagationBasedCallGraphAnalysisScheduler,
            new SimpleInstantiatedTypesAnalysisScheduler(theSetEntitySelector),
            new TypePropagationAnalysisScheduler(theSetEntitySelector)
        )
    }
}

object XTACallGraphKey extends PropagationBasedCallGraphKey {
    override def setEntitySelector(): SetEntitySelector = XTASetEntitySelector
}

object MTACallGraphKey extends PropagationBasedCallGraphKey {
    override def setEntitySelector(): SetEntitySelector = MTASetEntitySelector
}

object FTACallGraphKey extends PropagationBasedCallGraphKey {
    override def setEntitySelector(): SetEntitySelector = FTASetEntitySelector
}

object CTACallGraphKey extends PropagationBasedCallGraphKey {
    override def setEntitySelector(): SetEntitySelector = CTASetEntitySelector
}