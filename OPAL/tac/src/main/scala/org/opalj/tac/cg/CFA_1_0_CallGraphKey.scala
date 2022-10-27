/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.JavaProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.JavaFPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.CallStringContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeIterator

/**
 * A [[JavaProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_0_CallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): JavaProjectInformationKeys = {
        TypeBasedPointsToCallGraphKey.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[JavaFPCFAnalysisScheduler] = {
        TypeBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }

    override def getTypeIterator(project: SomeProject): TypeIterator =
        new TypeIterator(project) with TypesBasedPointsToTypeIterator with CallStringContextProvider { val k = 1 }
}
