/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package cg

import org.opalj.si.ProjectInformationKey
import org.opalj.si.Project
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.value.Function

/**
 * Project information Key to retrieve the simple native call graph.
 *
 * @author Nicolas Gross
 */
object SimpleNativeCallGraphKey
    extends ProjectInformationKey[Project, SimpleNativeCallGraph, Set[Function]] {

    /**
     * The computation of the call graph requires a LLVM project.
     *
     * @return the dependencies.
     */
    override def requirements(
        project: Project
    ): Seq[ProjectInformationKey[_ <: Project, _ <: AnyRef, _ <: AnyRef]] = Seq(LLVMProjectKey)

    /**
     * Computes the call graph.
     *
     * Expects a list of entry points as project key initialization data:
     * <pre>
     * project.updateProjectInformationKeyInitializationData(SimpleCallGraphKey)(
     * current => Set(llvmProject.function("main").get)
     * )
     * </pre>
     *
     * @return the call graph.
     */
    override def compute(project: Project): SimpleNativeCallGraph = {
        val manager = project.get(FPCFAnalysesManagerKey)
        val (ps, _) = manager.runAll(LazySimpleCallGraphAnalysis)
        val entryPoints =
            project.getOrCreateProjectInformationKeyInitializationData(this, Set.empty[Function])
        ps(EPK(entryPoints, SimpleNativeCallGraph.key)) match {
            case ep: FinalEP[Set[Function], SimpleNativeCallGraph] => ep.p
            case _                                                 => throw new RuntimeException("unexpected error while computing SimpleNativeCallGraph")
        }
    }
}
