/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.scheduling

import org.opalj.si.{MetaProject, ProjectInformationKey, ProjectInformationKeys, PropertyStoreKey}

/**
 * The ''key'' object to get the [[FPCFAnalysesManager]].
 *
 * @example
 *      To get an instance of the [[FPCFAnalysesManager]] pass this key to a
 *      project's `get` method.
 * @author Michael Reif
 */
object FPCFAnalysesManagerKey extends ProjectInformationKey[MetaProject, FPCFAnalysesManager, Nothing] {

    override def requirements(project: MetaProject): ProjectInformationKeys[MetaProject] = List(PropertyStoreKey)

    override def compute(project: MetaProject): FPCFAnalysesManager = new FPCFAnalysesManager(project)

}
