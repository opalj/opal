/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.analyses.SimpleContextProvider
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 *  An [[org.opalj.br.analyses.ProjectInformationKey]] to get the
 *  [[org.opalj.br.fpcf.analyses.ContextProvider]] used to compute the current project's call graph.
 *  This key is intended to be set up by a corresponding [[org.opalj.tac.cg.TypeIteratorKey]].
 */
object ContextProviderKey extends ProjectInformationKey[ContextProvider, ContextProvider] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Nil

    override def compute(theProject: SomeProject): ContextProvider = {
        theProject.getProjectInformationKeyInitializationData(this) match {
            case Some(contextProvider: ContextProvider) => contextProvider
            case None =>
                implicit val logContext: LogContext = theProject.logContext
                OPALLogger.warn(
                    "analysis configuration",
                    s"no context provider configured, using SimpleContextProvider as a fallback"
                )
                new SimpleContextProvider {
                    override val project: SomeProject = theProject
                }
        }
    }
}
