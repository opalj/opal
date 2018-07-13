/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.ProjectInformationKey

/**
 * The ''key'' object to get the interface methods for which call by signature resolution
 * needs to be done.
 *
 * @note   To get call by signature information use the [[org.opalj.br.analyses.Project]]'s `get`
 *         method and pass in `this` object.
 *
 * @see    [[CallBySignatureResolution]] for further information.
 *
 * @author Michael Reif
 */
object CallBySignatureResolutionKey
    extends ProjectInformationKey[CallBySignatureResolution, Nothing] {

    /**
     * The computation of [[CallBySignatureResolution]] information needs the
     * [[org.opalj.br.analyses.ProjectIndex]].
     */
    protected def requirements = List(ProjectIndexKey)

    /**
     * Computes the [[CallBySignatureResolution]] for the given project.
     */
    override protected def compute(project: SomeProject): CallBySignatureResolution = {
        CallBySignatureResolution(project)
    }
}
