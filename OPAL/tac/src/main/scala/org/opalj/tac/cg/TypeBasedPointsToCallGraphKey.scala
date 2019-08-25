/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.TypeBasedPointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object TypeBasedPointsToCallGraphKey extends AbstractCallGraphKey {

    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[FPCFAnalysisScheduler] = {
        List(
            TypeBasedPointsToBasedCallGraphAnalysisScheduler,
            TypeBasedPointsToAnalysisScheduler
        //ConfiguredNativeMethodsPointsToAnalysisScheduler,
        //DoPrivilegedPointsToCGAnalysisScheduler
        )
    }

}
