/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.old.taint

import org.opalj.br.Method
import org.opalj.tac.fpcf.analyses.ifds.old.UnbalancedReturnFact
import org.opalj.tac.fpcf.analyses.ifds.taint.Fact

/**
 * The unbalanced return fact of this analysis.
 *
 * @param index The index, at which the analyzed method is called by some caller.
 * @param innerFact The fact, which will hold in the caller context after the call.
 * @param callChain The current call chain from the sink.
 */
case class UnbalancedTaintFact(index: Int, innerFact: Fact, callChain: Seq[Method])
    extends UnbalancedReturnFact[Fact] with Fact

trait TaintProblem[C, Statement, IFDSFact] {

    /**
     * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
     * In this case, no return flow facts will be created.
     *
     * @param callee The method, which was called.
     * @return True, if the method is a sanitizer.
     */
    protected def sanitizesReturnValue(callee: C): Boolean

    /**
     * Called in callToReturnFlow. This method can return facts, which will be removed after
     * `callee` was called. I.e. the method could sanitize parameters.
     *
     * @param call The call statement.
     * @param in The facts, which hold before the call.
     * @return Facts, which will be removed from `in` after the call.
     */
    protected def sanitizeParameters(call: Statement, in: Set[IFDSFact]): Set[IFDSFact]
}
