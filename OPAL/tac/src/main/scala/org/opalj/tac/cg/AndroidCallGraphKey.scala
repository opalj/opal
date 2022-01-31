/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.{CHATypeProvider, RTATypeProvider, TypeProvider, eagerAndroidICCAnalysisScheduler}
import org.opalj.tac.fpcf.analyses.cg.rta.{ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler, InstantiatedTypesAnalysisScheduler}

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] for Android based on the
 * [[org.opalj.br.analyses.ProjectInformationKey]] Parameter cgk. Currently only supports CHA and RTA.
 *
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * Does not work for Androidx
 *
 * @param cgk Need to specify the key of the call graph algorithm that should be used e.g. CHACallGraphKey
 * @param manifestPath Path to the projects AndroidManifest.xml
 *
 * @author Tom Nikisch
 */
class AndroidCallGraphKey(cgk: ProjectInformationKey[CallGraph, Nothing], manifestPath: String) extends CallGraphKey {
    /**
     * Lists the call graph specific schedulers that must be run to compute the respective call
     * graph.
     *
     */
    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        val AndroidICC = eagerAndroidICCAnalysisScheduler
        AndroidICC.setManifest(manifestPath)
        if (cgk == CHACallGraphKey) {
            return List(
                AndroidICC
            )
        }
        if (cgk == RTACallGraphKey) {
            List(
                AndroidICC,
                InstantiatedTypesAnalysisScheduler,
                ConfiguredNativeMethodsInstantiatedTypesAnalysisScheduler
            )
        } else null
    }

    override def getTypeProvider(project: SomeProject): TypeProvider = {
        if(cgk == CHACallGraphKey){
            new CHATypeProvider(project)
        }
        else new RTATypeProvider(project)
    }
}
