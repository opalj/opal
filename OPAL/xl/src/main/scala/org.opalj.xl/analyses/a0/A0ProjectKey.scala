/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a0

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}

object A0ProjectKey extends ProjectInformationKey[A0Project, Iterable[String]] {
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): A0Project = {
        A0Project(
            project.getOrCreateProjectInformationKeyInitializationData(this,
                Iterable.empty))
    }

}