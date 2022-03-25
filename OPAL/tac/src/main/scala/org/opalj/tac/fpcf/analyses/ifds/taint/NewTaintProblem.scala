/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

trait NewTaintProblem[C, Statement, IFDSFact] {

  /**
   * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
   * In this case, no return flow facts will be created.
   *
   * @param callee The method, which was called.
   * @return True, if the method is a sanitizer.
   */
  protected def sanitizesReturnValue(callee: C): Boolean

  /**
   * Called in callToReturnFlow. This method can return whether the input fact
   * will be removed after `callee` was called. I.e. the method could sanitize parameters.
   *
   * @param call The call statement.
   * @param in The fact which holds before the call.
   * @return Whether in will be removed after the call.
   */
  protected def sanitizesParameter(call: Statement, in: IFDSFact): Boolean
}