/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectInformationKey

/**
 * Key that can be used to get a `DependencyStore` that contains all dependencies.
 *
 * ==Usage==
 * Just pass this object to a `Project` to get the [[DependencyStore]].
 *
 * @author Michael Eichberg
 */
object DependencyStoreKey extends ProjectInformationKey[DependencyStore, Nothing] {

    override def requirements(project: SomeProject): Seq[ProjectInformationKey[_ <: AnyRef, Nothing]] = Nil

    override def compute(project: SomeProject): DependencyStore = {
        DependencyStore(project.allClassFiles)(project.logContext)
    }
}
