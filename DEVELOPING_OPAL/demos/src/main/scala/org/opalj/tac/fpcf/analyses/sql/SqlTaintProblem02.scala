
package org.opalj.tac.fpcf.analyses.sql

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.{JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.ForwardTaintProblem
import org.opalj.tac.fpcf.properties.{FlowFact, Taint, TaintFact, TaintNullFact, Variable}

class SqlTaintAnalysis02(project: SomeProject)
  extends IFDSAnalysis()(project, new SqlTaintProblem02(project), Taint)

class SqlTaintProblem02(p: SomeProject)  extends ForwardTaintProblem(p){
  /**
   * Called, when the exit to return facts are computed for some `callee` with the null fact and
   * the callee's return value is assigned to a variable.
   * Creates a taint, if necessary.
   *
   * @param callee The called method.
   * @param call   The call.
   * @return Some variable fact, if necessary. Otherwise none.
   */
  override protected def createTaints(callee: Method, call: JavaStatement): Set[TaintFact] =
    if (callee.name == "source") Set(Variable(call.index))
    else Set.empty

  /**
   * Called, when the call to return facts are computed for some `callee`.
   * Creates a FlowFact, if necessary.
   *
   * @param callee The method, which was called.
   * @param call   The call.
   * @return Some FlowFact, if necessary. Otherwise None.
   */
  override protected def createFlowFact(callee: Method, call: JavaStatement, in: TaintFact): Option[FlowFact] =
    if (callee.name == "sink" && in == Variable(-2))
      Some(FlowFact(Seq(JavaMethod(call.method), JavaMethod(callee))))
    else None

  /**
   * The entry points of this analysis.
   */
  override def entryPoints: Seq[(Method, TaintFact)] =
    for {
      m <- p.allMethodsWithBody
      if m.name == "main"
    } yield m -> TaintNullFact

  /**
   * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
   * In this case, no return flow facts will be created.
   *
   * @param callee The method, which was called.
   * @return True, if the method is a sanitizer.
   */
  override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

  /**
   * Called in callToReturnFlow. This method can return whether the input fact
   * will be removed after `callee` was called. I.e. the method could sanitize parameters.
   *
   * @param call The call statement.
   * @param in   The fact which holds before the call.
   * @return Whether in will be removed after the call.
   */
  override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

  override def callToReturnFlow(call: JavaStatement, in: TaintFact, successor: JavaStatement): Set[TaintFact] = {
    print("index: "+call.index +" ")
    println(in)
    super.callToReturnFlow(call, in, successor)
  }
}
