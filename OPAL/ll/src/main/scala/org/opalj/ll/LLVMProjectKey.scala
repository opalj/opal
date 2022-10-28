/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.br.analyses.{JavaProjectInformationKey, SomeProject}
import org.opalj.si.ProjectInformationKey

object LLVMProjectKey extends JavaProjectInformationKey[LLVMProject, Iterable[String]] {
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[SomeProject, Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): LLVMProject = {
        LLVMProject(project.getOrCreateProjectInformationKeyInitializationData(this, Iterable.empty))
    }

}
