/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.si.ProjectInformationKey
import org.opalj.si.Project

object LLVMProjectKey extends ProjectInformationKey[Project, LLVMProject, Iterable[String]] {
    override def requirements(project: Project): Seq[ProjectInformationKey[Project, Nothing, Nothing]] = Nil

    override def compute(project: Project): LLVMProject = {
        LLVMProject(project.getOrCreateProjectInformationKeyInitializationData(this, Iterable.empty))
    }

}
