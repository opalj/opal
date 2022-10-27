/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.JavaProjectInformationKey
import org.opalj.tac.fpcf.analyses.cg.CHATypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypeIterator

/**
 *  An [[JavaProjectInformationKey]] to get the [[TypeIterator]] used to compute
 *  the current project's call graph.
 *  This key is intended to be set up by a corresponding [[org.opalj.tac.cg.CallGraphKey]].
 */
object TypeIteratorKey
    extends JavaProjectInformationKey[TypeIterator, () => TypeIterator] {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = Nil

    override def compute(project: SomeProject): TypeIterator = {
        project.getProjectInformationKeyInitializationData(this) match {
            case Some(init) =>
                init()
            case None =>
                implicit val logContext: LogContext = project.logContext
                OPALLogger.warn(
                    "analysis configuration",
                    s"no type iterator configured, using CHA as a fallback"
                )
                new CHATypeIterator(project)
        }
    }
}
