/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.cg

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.cg.PhasarCallGraphParser.PhasarCallGraph

object PhasarCallGraphKey extends ProjectInformationKey[PhasarCallGraph, String] {

    /**
     * The computation/parsing of the phasar call graph requires a LLVM project.
     *
     * @return the dependencies.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[_ <: AnyRef, _ <: AnyRef]] = Seq(LLVMProjectKey)

    /**
     * Computes/parses the phasar JSON call graph.
     *
     * Expects the path to the phasar JSON output to be already set as project key  initialization data:
     * <pre>
     * project.updateProjectInformationKeyInitializationData(PhasarCallGraphKey)(
     * current => "path_to_call_graph_json"
     * )
     * </pre>
     *
     * @return the call graph.
     */
    override def compute(project: SomeProject): PhasarCallGraph = PhasarCallGraphParser.parse(
        project.getOrCreateProjectInformationKeyInitializationData(this, ""),
        project.get(LLVMProjectKey))

}