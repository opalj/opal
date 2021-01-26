/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on class
 * hierarchy analysis (CHA).
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object CHACallGraphKey extends AbstractCallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        super.requirements(project) :+ IsOverridableMethodKey
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(CHACallGraphAnalysisScheduler)
    }

}
