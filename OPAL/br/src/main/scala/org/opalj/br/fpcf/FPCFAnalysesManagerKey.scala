/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

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

    override def requirements(project: SomeProject): ProjectInformationKeys = List(PropertyStoreKey)

    override def compute(project: SomeProject): FPCFAnalysesManager = new FPCFAnalysesManager(project)

}
