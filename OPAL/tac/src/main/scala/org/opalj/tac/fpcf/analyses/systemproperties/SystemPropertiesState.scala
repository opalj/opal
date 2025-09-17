/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package systemproperties

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.properties.TACAI

/**
 * @see [[SystemPropertiesAnalysis]]
 * @author Maximilian Rüsch
 */
final class SystemPropertiesState[ContextType <: Context](
    override val callContext:                  ContextType,
    override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends BaseAnalysisState with TACAIBasedAnalysisState[ContextType] {

    private[this] var _stringConstancyDependees: Map[
        VariableContext,
        EOptionP[VariableContext, StringConstancyProperty]
    ] =
        Map.empty

    def updateStringDependee(dependee: EOptionP[VariableContext, StringConstancyProperty]): Unit = {
        _stringConstancyDependees = _stringConstancyDependees.updated(dependee.e, dependee)
    }

    override def hasOpenDependencies: Boolean = {
        super.hasOpenDependencies ||
        _stringConstancyDependees.valuesIterator.exists(_.isRefinable)
    }

    override def dependees: Set[SomeEOptionP] =
        super.dependees ++ _stringConstancyDependees.valuesIterator.filter(_.isRefinable)
}
