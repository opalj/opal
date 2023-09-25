/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

/**
 *  An [[org.opalj.br.analyses.ProjectInformationKey]] to get the [[TypeIterator]] used to compute
 *  the current project's call graph.
 *  This key is intended to be set up by a corresponding [[org.opalj.tac.cg.CallGraphKey]].
 */
object TypeIteratorKey
    extends ProjectInformationKey[TypeIterator, TypeIterator] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Nil

    override def compute(project: SomeProject): TypeIterator = {

        val typeIterator = project.getProjectInformationKeyInitializationData(this) match {
            case Some(typeIterator: TypeIterator) => typeIterator
            case None =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.warn(
                    "analysis configuration",
                    s"no type iterator configured, using CHA as a fallback"
                )
                new CHATypeIterator(project)
        }

        project.updateProjectInformationKeyInitializationData(ContextProviderKey) {
            case Some(contextProvider: ContextProvider) if contextProvider ne typeIterator =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.error(
                    "analysis configuration",
                    s"must not configure multiple type iterators"
                )
                throw new IllegalArgumentException()
            case Some(_) => typeIterator
            case None    => typeIterator
        }

        project.get(ContextProviderKey).asInstanceOf[TypeIterator]
    }
}
