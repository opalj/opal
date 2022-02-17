/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.AnalysisState
import org.opalj.tac.fpcf.analyses.cg.ContextualAnalysis
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A base class for the state of FPCF analyses operating on the three-address code of methods.
 *
 * @author Florian Kuebler
 */
trait TACAIBasedAnalysisState[TheContextType <: Context]
    extends AnalysisState with ContextualAnalysis {

    override type ContextType = TheContextType

    def callContext: ContextType

    protected[this] var _tacDependee: EOptionP[Method, TACAI]
    assert((_tacDependee eq null) || (_tacDependee.hasUBP && _tacDependee.ub.tac.isDefined))

    abstract override def hasOpenDependencies: Boolean = {
        (_tacDependee ne null) && _tacDependee.isRefinable || super.hasOpenDependencies
    }
    final def hasTACDependee: Boolean = _tacDependee.isRefinable

    /**
     * Inherited classes that introduce new dependencies must override this method and call add a
     * call to super!
     */
    abstract override def dependees: Set[SomeEOptionP] = {
        val otherDependees = super.dependees
        if ((_tacDependee ne null) && _tacDependee.isRefinable)
            otherDependees + _tacDependee
        else
            otherDependees
    }

    final def updateTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        _tacDependee = tacDependee
    }

    final def tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = {
        _tacDependee.ub.tac.get
    }

    final def tacDependee: EOptionP[Method, TACAI] = {
        _tacDependee
    }

}
