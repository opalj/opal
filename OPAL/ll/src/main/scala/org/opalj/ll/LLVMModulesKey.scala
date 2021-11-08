/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}

object LLVMModulesKey extends ProjectInformationKey[Iterable[Module], Iterable[String]] {
    /**
     * The [[LLVMModulesKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): Iterable[Module] = {
        val modules_paths = project.getOrCreateProjectInformationKeyInitializationData(this, Iterable.empty)
        modules_paths.map(path ⇒ Reader.readIR(path).get)
    }

}
