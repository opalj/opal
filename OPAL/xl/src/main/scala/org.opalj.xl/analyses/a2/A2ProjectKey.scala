/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package xl
package analyses
package a2

import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}

object A2ProjectKey extends ProjectInformationKey[A2Project, Iterable[String]] {
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    override def compute(project: SomeProject): A2Project = {
        A2Project(
            project.getOrCreateProjectInformationKeyInitializationData(this,
                Iterable.empty))
    }

}