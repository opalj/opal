/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.TypeBasedPointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.TypeBasedDoPrivilegedPointsToCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.pointsto.TypeBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedArraycopyPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedConfiguredMethodsPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedTamiFlexPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedUnsafePointsToAnalysisScheduler

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
            TypeBasedPointsToAnalysisScheduler,
            TypeBasedConfiguredMethodsPointsToAnalysisScheduler,
            TypeBasedDoPrivilegedPointsToCGAnalysisScheduler,
            TypeBasedTamiFlexPointsToAnalysisScheduler,
            TypeBasedArraycopyPointsToAnalysisScheduler,
            TypeBasedUnsafePointsToAnalysisScheduler,
            TypeBasedPointsToBasedThreadRelatedCallsAnalysisScheduler
        )
    }

}
