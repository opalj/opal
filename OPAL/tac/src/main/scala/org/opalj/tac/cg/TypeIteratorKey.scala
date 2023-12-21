/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

/**
 *  An [[org.opalj.br.analyses.ProjectInformationKey]] to get the [[TypeIterator]] used to compute
 *  the current project's call graph.
 *  This key is intended to be set up by a corresponding [[org.opalj.tac.cg.CallGraphKey]].
 */
object TypeIteratorKey extends ProjectInformationKey[TypeIterator, TypeIterator] {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        implicit val logContext: LogContext = project.logContext

        project.getProjectInformationKeyInitializationData(this) match {
            case Some(_) =>
                OPALLogger.error(
                    "analysis configuration",
                    "TypeIteratorKey has no initialization data, configure ContextProviderKey instead"
                )
            case _ =>
        }

        project.getProjectInformationKeyInitializationData(ContextProviderKey) match {
            case Some(_: TypeIterator) =>
            case Some(_: ContextProvider) =>
                OPALLogger.error(
                    "analysis configuration",
                    "a context provider has already been established"
                )
                throw new IllegalStateException()
            case None =>
                OPALLogger.warn(
                    "analysis configuration",
                    "no type iterator configured, using CHA as a fallback"
                )
                project.updateProjectInformationKeyInitializationData(ContextProviderKey) {
                    _ => new CHATypeIterator(project)
                }
        }

        Seq(ContextProviderKey)
    }

    override def compute(project: SomeProject): TypeIterator = {
        project.get(ContextProviderKey).asInstanceOf[TypeIterator]
    }
}
