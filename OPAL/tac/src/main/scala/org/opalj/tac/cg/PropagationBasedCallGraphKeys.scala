/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.fpcf.ComputationSpecification
import org.opalj.tac.fpcf.analyses.cg.rta.ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.ArrayInstantiationsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.CTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.FTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.InstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.LibraryInstantiatedTypesBasedEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.xta.MTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.PropagationBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.SetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTASetEntitySelector

/**
  * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on Tip and
  * Palsberg's propagation-based algorithms.
  *
  * @see [[AbstractCallGraphKey]] for further details.
  *
  * If the [[org.opalj.br.analyses.cg.LibraryEntryPointsFinder]] is scheduled
  * the analysis will schedule
  * [[org.opalj.tac.fpcf.analyses.cg.xta.LibraryInstantiatedTypesBasedEntryPointsAnalysis]].
  *
  * Note, that initial instantiated types ([[org.opalj.br.analyses.cg.InitialInstantiatedTypesKey]])
  * and entry points ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) can be configured before
  * hand.
  * Furthermore, you can configure the analysis mode (Library or Application) in the configuration
  * of these keys.
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

        val common = List(
            new PropagationBasedCallGraphAnalysisScheduler(theSetEntitySelector),
            new InstantiatedTypesAnalysisScheduler(theSetEntitySelector),
            new ArrayInstantiationsAnalysisScheduler(theSetEntitySelector),
            new TypePropagationAnalysisScheduler(theSetEntitySelector),
            ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
        )

        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"

        if (isLibrary) {
            LibraryInstantiatedTypesBasedEntryPointsAnalysis :: common
        } else {
            common
        }
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