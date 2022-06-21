/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.fpcf.analyses.cg.rta.ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.ArrayInstantiationsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.CTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.FTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.InstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.LibraryInstantiatedTypesBasedEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.xta.MTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.TypeSetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.XTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.PropagationBasedTypeProvider

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on Tip and
 * Palsberg's propagation-based algorithms.
 *
 * @see [[CallGraphKey]] for further details.
 *
 *      If the [[org.opalj.br.analyses.cg.LibraryEntryPointsFinder]] is scheduled
 *      the analysis will schedule
 *      [[org.opalj.tac.fpcf.analyses.cg.xta.LibraryInstantiatedTypesBasedEntryPointsAnalysis]].
 *
 *      Note, that initial instantiated types ([[org.opalj.br.analyses.cg.InitialInstantiatedTypesKey]])
 *      and entry points ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) can be configured before
 *      hand.
 *      Furthermore, you can configure the analysis mode (Library or Application) in the configuration
 *      of these keys.
 *
 * @author Andreas Bauer
 */
trait PropagationBasedCallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        super.requirements(project) ++ Seq(InitialInstantiatedTypesKey, SimpleContextsKey)
    }

    def typeSetEntitySelector(): TypeSetEntitySelector

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        val theTypeSetEntitySelector = typeSetEntitySelector()

        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"

        List(
            new InstantiatedTypesAnalysisScheduler(theTypeSetEntitySelector),
            new ArrayInstantiationsAnalysisScheduler(theTypeSetEntitySelector),
            new TypePropagationAnalysisScheduler(theTypeSetEntitySelector),
            ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
        ) ::: (if (isLibrary) List(LibraryInstantiatedTypesBasedEntryPointsAnalysis) else Nil)
    }

    override def getTypeProvider(project: SomeProject) =
        new PropagationBasedTypeProvider(project, typeSetEntitySelector())
}

object XTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typeSetEntitySelector(): TypeSetEntitySelector = XTASetEntitySelector
}

object MTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typeSetEntitySelector(): TypeSetEntitySelector = MTASetEntitySelector
}

object FTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typeSetEntitySelector(): TypeSetEntitySelector = FTASetEntitySelector
}

object CTACallGraphKey extends PropagationBasedCallGraphKey {
    override def typeSetEntitySelector(): TypeSetEntitySelector = CTASetEntitySelector
}