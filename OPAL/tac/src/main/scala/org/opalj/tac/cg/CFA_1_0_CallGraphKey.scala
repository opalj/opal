/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.CallStringContextProvider
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.analyses.cg.TypesBasedPointsToTypeProvider

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 *
 * @see [[CallGraphKey]] for further details.
 *
 * @author Dominik Helm
 */
object CFA_1_0_CallGraphKey extends CallGraphKey {

    override def requirements(project: SomeProject): ProjectInformationKeys = {
        TypeBasedPointsToCallGraphKey.requirements(project)
    }

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Iterable[FPCFAnalysisScheduler] = {
        TypeBasedPointsToCallGraphKey.callGraphSchedulers(project)
    }

    override def getTypeProvider(project: SomeProject): TypeProvider =
        new TypeProvider(project) with TypesBasedPointsToTypeProvider with CallStringContextProvider { val k = 1 }
}
