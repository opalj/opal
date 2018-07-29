/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectInformationKey

/**
 * The ''key'' object to get the [[FPCFAnalysesManager]].
 *
 * @example
 *      To get an instance of the [[FPCFAnalysesManager]] pass this key to a
 *      project's `get` method.
 *
 * @author Michael Reif
 */
object FPCFAnalysesManagerKey extends ProjectInformationKey[FPCFAnalysesManager, Nothing] {

    protected def requirements = List(PropertyStoreKey)

    protected def compute(project: SomeProject): FPCFAnalysesManager = {
        new FPCFAnalysesManager(project)
    }
}
