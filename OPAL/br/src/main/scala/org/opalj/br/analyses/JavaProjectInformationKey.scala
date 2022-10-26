/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses

import org.opalj.si.{MetaProject, ProjectInformationKey}

trait JavaProjectInformationKey[T <: AnyRef, I <: AnyRef] extends ProjectInformationKey[SomeProject, T, I] {

    final override def compute(project: MetaProject): T = {
        project match {
            case someProject: SomeProject => compute(someProject)
            case _                        => throw new IllegalArgumentException("Wrong Project Kind") // TODO: Be more graceful
        }
    }

    def compute(project: SomeProject): T

    final override def requirements(project: MetaProject): JavaProjectInformationKeys = {
        project match {
            case someProject: SomeProject => requirements(someProject)
            case _                        => throw new IllegalArgumentException("Wrong Project Kind") // TODO: Be more graceful
        }
    }

    def requirements(project: SomeProject): JavaProjectInformationKeys
}