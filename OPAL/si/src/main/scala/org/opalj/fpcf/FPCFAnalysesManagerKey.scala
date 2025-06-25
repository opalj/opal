/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.si.Project
import org.opalj.si.ProjectInformationKey
import org.opalj.si.ProjectInformationKeys

/**
 * The ''key'' object to get the [[FPCFAnalysesManager]].
 *
 * @example
 *      To get an instance of the [[FPCFAnalysesManager]] pass this key to a
 *      project's `get` method.
 *
 * @author Michael Reif
 */
object FPCFAnalysesManagerKey extends ProjectInformationKey[Project, FPCFAnalysesManager, Nothing] {

    override def requirements(project: Project): ProjectInformationKeys = List(PropertyStoreKey)

    override def compute(project: Project): FPCFAnalysesManager = new FPCFAnalysesManager(project)

}
