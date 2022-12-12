/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a1

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}

object A1ProjectKey extends ProjectInformationKey[A1Project, Iterable[String]] {
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): A1Project = {
        A1Project(
            project.getOrCreateProjectInformationKeyInitializationData(this,
                Iterable.empty))
    }

}