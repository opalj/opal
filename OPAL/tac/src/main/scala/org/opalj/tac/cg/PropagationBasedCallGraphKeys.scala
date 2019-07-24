/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.xta.AbstractTypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.AttachToClassFile
import org.opalj.tac.fpcf.analyses.cg.xta.AttachToDefinedMethod
import org.opalj.tac.fpcf.analyses.cg.xta.PerMethodTypeSetEntity
import org.opalj.tac.fpcf.analyses.cg.xta.MTATypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.PropagationBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.SimpleInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTATypePropagationAnalysisScheduler

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

    def directInstantiationSetEntityDecision(): PerMethodTypeSetEntity
    def typePropagationAnalysis(): AbstractTypePropagationAnalysisScheduler

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        // TODO AB Configure this properly depending on library/application (see: RTA key)
        List(
            PropagationBasedCallGraphAnalysisScheduler,
            new SimpleInstantiatedTypesAnalysisScheduler(directInstantiationSetEntityDecision()),
            typePropagationAnalysis()
        )
    }
}

object XTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typePropagationAnalysis(): AbstractTypePropagationAnalysisScheduler =
        XTATypePropagationAnalysisScheduler

    override def directInstantiationSetEntityDecision(): PerMethodTypeSetEntity = AttachToDefinedMethod
}

object MTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typePropagationAnalysis(): AbstractTypePropagationAnalysisScheduler =
        MTATypePropagationAnalysisScheduler

    override def directInstantiationSetEntityDecision(): PerMethodTypeSetEntity = AttachToClassFile
}