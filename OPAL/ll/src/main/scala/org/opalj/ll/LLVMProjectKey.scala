/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

object LLVMProjectKey extends ProjectInformationKey[LLVMProject, Iterable[String]] {
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): LLVMProject = {
        LLVMProject(project.getOrCreateProjectInformationKeyInitializationData(this, Iterable.empty))
    }

}
