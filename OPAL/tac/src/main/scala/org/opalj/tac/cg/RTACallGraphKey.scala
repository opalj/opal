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
import org.opalj.tac.fpcf.analyses.cg.rta.InstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.LibraryInstantiatedTypesBasedEntryPointsAnalysis
import org.opalj.tac.fpcf.analyses.cg.RTATypeProvider

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on rapid type
 * analysis (RTA).
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
 * @author Florian Kuebler
 */
object RTACallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        Seq(InitialInstantiatedTypesKey, SimpleContextsKey) ++: super.requirements(project)
    }

    override def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        // in case the library entrypoints finder is configured, we want to use the
        // EagerLibraryEntryPointsAnalysis
        val isLibrary =
            project.config.getString("org.opalj.br.analyses.cg.InitialEntryPointsKey.analysis") ==
                "org.opalj.br.analyses.cg.LibraryEntryPointsFinder"

        List(
            InstantiatedTypesAnalysisScheduler,
            ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
        ) ::: (if (isLibrary) List(LibraryInstantiatedTypesBasedEntryPointsAnalysis) else Nil)
    }

    override def getTypeProvider(project: SomeProject) = new RTATypeProvider(project)
}
