/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.ComputationSpecification
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.tac.fpcf.analyses.cg.pointsto.PointsToBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.DoPrivilegedPointsToCGAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.TypeBasedPointsToAnalysisScheduler
import org.opalj.tac.fpcf.analyses.pointsto.ConfiguredNativeMethodsPointsToAnalysisScheduler

/**
 * A [[org.opalj.br.analyses.ProjectInformationKey]] to compute a [[CallGraph]] based on
 * the points-to analysis.
 * @see [[AbstractCallGraphKey]] for further details.
 *
 * @author Florian Kuebler
 */
object PointsToCallGraphKey extends AbstractCallGraphKey {
    override protected def callGraphSchedulers(
        project: SomeProject
    ): Traversable[ComputationSpecification[FPCFAnalysis]] = {
        List(
            PointsToBasedCallGraphAnalysisScheduler,
            TypeBasedPointsToAnalysisScheduler,
            ConfiguredNativeMethodsPointsToAnalysisScheduler,
            DoPrivilegedPointsToCGAnalysisScheduler
        )
    }
}
