/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.opalj.br.analyses.{ProjectInformationKey, ProjectInformationKeys, SomeProject}

object LLVMFunctionsKey extends ProjectInformationKey[Iterable[Function], Nothing] {
    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(LLVMModulesKey)

    override def compute(project: SomeProject): Iterable[Function] = {
      project.get(LLVMModulesKey).flatMap(module => module.functions())
    }

}
