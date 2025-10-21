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

    override def requirements(theProject: SomeProject): ProjectInformationKeys = {
        val provider = theProject.getOrCreateProjectInformationKeyInitializationData(
            this, {
                OPALLogger.warn(
                    "analysis configuration",
                    s"no context provider configured, using SimpleContextProvider as a fallback"
                )(using theProject.logContext)

                new SimpleContextProvider {
                    override val project: SomeProject = theProject
                }
            }
        )

        provider.requiredProjectInformation
    }

    override def compute(theProject: SomeProject): ContextProvider = {
        implicit val logContext: LogContext = theProject.logContext
        theProject.getProjectInformationKeyInitializationData(this) match {
            case Some(s) =>
                OPALLogger.info(
                    "analysis configuration",
                    s"ContextProvider $s is selected"
                )
                s
            case None =>
                OPALLogger.error(
                    "analysis configuration",
                    s"no context provider configured even though requirements were run"
                )
                throw new IllegalStateException()
        }
    }
}
