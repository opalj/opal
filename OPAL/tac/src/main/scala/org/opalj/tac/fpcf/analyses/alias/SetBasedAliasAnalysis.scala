/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.alias

import org.opalj.br.fpcf.properties.alias.MayAlias
import org.opalj.br.fpcf.properties.alias.MustAlias
import org.opalj.br.fpcf.properties.alias.NoAlias
import org.opalj.fpcf.ProperPropertyComputationResult

trait SetBasedAliasAnalysis extends AbstractAliasAnalysis {

    protected[this] type AliasSet <: AliasSetLike[_, AliasSet]

    override protected[this] type AnalysisState <: SetBasedAliasAnalysisState[_, AliasSet]

    protected[this] def createResult()(
        implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): ProperPropertyComputationResult = {

        val pointsTo1 = state.pointsTo1
        val pointsTo2 = state.pointsTo2

        val intersection = pointsTo1.intersection(pointsTo2)

        if (intersection.isEmpty) {
            return if (state.hasDependees) interimResult(NoAlias, MayAlias) else result(NoAlias)
        } else if (!intersection.pointsToAny && checkMustAlias(intersection)) {
            return if (state.hasDependees) interimResult(MustAlias, MayAlias) else result(MustAlias)
        }

        result(MayAlias)
    }

    /**
     * Checks if the given intersection of points-to sets can be a must alias.
     * This method always returns false and should be overriden if more precise must alias checks can be performed
     */
    protected[this] def checkMustAlias(intersection: AliasSet)(implicit
        state:   AnalysisState,
        context: AnalysisContext
    ): Boolean = false
}
