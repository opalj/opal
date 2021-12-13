/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.CHATypeProvider

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on class
 * hierarchy analysis (CHA).
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object CHACallGraphKey extends CallGraphKey {

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = List.empty

    override def getTypeProvider(project: SomeProject) = new CHATypeProvider(project)

}
