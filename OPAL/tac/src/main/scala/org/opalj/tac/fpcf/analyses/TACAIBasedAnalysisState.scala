/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.value.ValueInformation
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A base class for the state of FPCF analyses operating on the three-address code of methods.
 *
 * @author Florian Kuebler
 */
trait TACAIBasedAnalysisState {

    def method: DefinedMethod

    protected[this] var _tacDependee: EOptionP[Method, TACAI]
    assert(_tacDependee.hasUBP && _tacDependee.ub.tac.isDefined)

    /**
     * Inherited classes that introduce new dependencies must override this method and call add a
     * call to super!
     */
    def hasOpenDependencies: Boolean = _tacDependee.isRefinable

    /**
     * Inherited classes that introduce new dependencies must override this method and call add a
     * call to super!
     */
    def dependees: List[SomeEOptionP] = if (_tacDependee.isRefinable)
        List(_tacDependee)
    else
        Nil

    final def updateTACDependee(tacDependee: EOptionP[Method, TACAI]): Unit = {
        _tacDependee = tacDependee
    }

    final def tac: TACode[TACMethodParameter, DUVar[ValueInformation]] = {
        _tacDependee.ub.tac.get
    }
}
