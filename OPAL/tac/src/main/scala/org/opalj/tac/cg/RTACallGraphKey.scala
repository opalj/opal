/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.rta.EagerLibraryInstantiatedTypesBasedEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.InstantiatedTypesAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on rapid type
 * analysis (RTA).
 *
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * If the [[org.opalj.br.analyses.cg.LibraryEntryPointsFinder]] is scheduled
 * the analysis will schedule
 * [[org.opalj.tac.fpcf.analyses.cg.rta.EagerLibraryInstantiatedTypesBasedEntryPointsAnalysis]].
 *
 * Note, that initial instantiated types ([[org.opalj.br.analyses.cg.InitialInstantiatedTypesKey]])
 * and entry points ([[org.opalj.br.analyses.cg.InitialEntryPointsKey]]) can be configured before
 * hand.
 * Furthermore, you can configure the analysis mode (Library or Application) in the configuration
 * of these keys.
 *
 * @author Florian Kuebler
 */
object RTACallGraphKey extends AbstractCallGraphKey {

    override protected def requirements: ProjectInformationKeys = {
        super.requirements :+ InitialInstantiatedTypesKey
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        // in case the library entrypoints finder is configured, we want to use the
        // EagerLibraryEntryPointsAnalysis
        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"
        if (isLibrary)
            List(
                EagerLibraryInstantiatedTypesBasedEntryPointsAnalysis,
                RTACallGraphAnalysisScheduler,
                InstantiatedTypesAnalysisScheduler,
                ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
            )
        else
            List(
                RTACallGraphAnalysisScheduler,
                InstantiatedTypesAnalysisScheduler,
                ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
            )
    }
}
