
package org.opalj.tac.fpcf.analyses.sql

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintProblem
import org.opalj.tac.fpcf.properties.TaintFact

class SqlTaintProblem02 (project: SomeProject) extends JavaIFDSProblem[TaintFact](project) with TaintProblem[Method, JavaStatement, TaintFact]{
  /**
   * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
   * In this case, no return flow facts will be created.
   *
   * @param callee The method, which was called.
   * @return True, if the method is a sanitizer.
   */
  override protected def sanitizesReturnValue(callee: Method): Boolean = ???

  /**
   * Called in callToReturnFlow. This method can return whether the input fact
   * will be removed after `callee` was called. I.e. the method could sanitize parameters.
   *
   * @param call The call statement.
   * @param in   The fact which holds before the call.
   * @return Whether in will be removed after the call.
   */
  override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = ???

  /**
   * The null fact of this analysis.
   */
  override def nullFact: TaintFact = ???

  /**
   * The entry points of this analysis.
   */
  override def entryPoints: Seq[(Method, TaintFact)] = ???

  /**
   * Computes the data flow for a normal statement.
   *
   * @param statement   The analyzed statement.
   * @param in          The fact which holds before the execution of the `statement`.
   * @param predecessor The predecessor of the analyzed `statement`, for which the data flow shall be
   *                    computed. Used for phi statements to distinguish the flow.
   * @return The facts, which hold after the execution of `statement` under the assumption
   *         that the facts in `in` held before `statement` and `successor` will be
   *         executed next.
   */
  override def normalFlow(statement: JavaStatement, in: TaintFact, predecessor: Option[JavaStatement]): Set[TaintFact] = ???

  /**
   * Computes the data flow for a call to start edge.
   *
   * @param call   The analyzed call statement.
   * @param callee The called method, for which the data flow shall be computed.
   * @param in     The fact which holds before the execution of the `call`.
   * @param source The entity, which is analyzed.
   * @return The facts, which hold after the execution of `statement` under the assumption that
   *         the facts in `in` held before `statement` and `statement` calls `callee`.
   */
  override def callFlow(call: JavaStatement, callee: Method, in: TaintFact): Set[TaintFact] = ???

  /**
   * Computes the data flow for an exit to return edge.
   *
   * @param call The statement, which called the `callee`.
   * @param exit The statement, which terminated the `callee`.
   * @param in   The fact which holds before the execution of the `exit`.
   * @return The facts, which hold after the execution of `exit` in the caller's context
   *         under the assumption that `in` held before the execution of `exit` and that
   *         `successor` will be executed next.
   */
  override def returnFlow(exit: JavaStatement, in: TaintFact, call: JavaStatement, callFact: TaintFact, successor: JavaStatement): Set[TaintFact] = ???

  /**
   * Computes the data flow for a call to return edge.
   *
   * @param call The statement, which invoked the call.
   * @param in   The facts, which hold before the `call`.
   * @return The facts, which hold after the call independently of what happens in the callee
   *         under the assumption that `in` held before `call`.
   */
  override def callToReturnFlow(call: JavaStatement, in: TaintFact, successor: JavaStatement): Set[TaintFact] = ???
}
