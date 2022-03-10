/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMStatement, NativeIFDSProblem}
import org.opalj.ll.llvm.Function

import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, NullFact, TaintProblem}

abstract class NativeForwardTaintProblem(project: SomeProject) extends NativeIFDSProblem[Fact](project) with TaintProblem[Function, LLVMStatement] {
  override def nullFact: Fact = NullFact

  /**
   * If a variable gets assigned a tainted value, the variable will be tainted.
   */
  override def normalFlow(statement: LLVMStatement, successor: LLVMStatement,
                          in: Set[Fact]): Set[Fact] =
    statement match {
      // TODO
      case _ ⇒ in
    }

  /**
   * Propagates tainted parameters to the callee. If a call to the sink method with a tainted
   * parameter is detected, no call-to-start
   * edges will be created.
   */
  override def callFlow(call: LLVMStatement, callee: Function,
                        in: Set[Fact], source: (Function, Fact)): Set[Fact] = {
    // TODO
    Set.empty[Fact]
  }

  /**
   * Taints an actual parameter, if the corresponding formal parameter was tainted in the callee.
   * If the callee's return value was tainted and it is assigned to a variable in the callee, the
   * variable will be tainted.
   * If a FlowFact held in the callee, this method will be appended to a new FlowFact, which holds
   * at this method.
   * Creates new taints and FlowFacts, if necessary.
   * If the sanitize method was called, nothing will be tainted.
   */
  override def returnFlow(call: LLVMStatement, callee: Function, exit: LLVMStatement,
                          successor: LLVMStatement, in: Set[Fact]): Set[Fact] = {
    // TODO
    Set.empty
  }

  /**
   * Removes taints according to `sanitizeParamters`.
   */
  override def callToReturnFlow(call: LLVMStatement, successor: LLVMStatement,
                                in:     Set[Fact],
                                source: (Function, Fact)): Set[Fact] =
    in -- sanitizeParameters(call, in)

  /**
   * Called, when the exit to return facts are computed for some `callee` with the null fact and
   * the callee's return value is assigned to a vairbale.
   * Creates a taint, if necessary.
   *
   * @param callee The called method.
   * @param call The call.
   * @return Some variable fact, if necessary. Otherwise none.
   */
  protected def createTaints(callee: Function, call: LLVMStatement): Set[Fact]

  /**
   * Called, when the call to return facts are computed for some `callee`.
   * Creates a FlowFact, if necessary.
   *
   * @param callee The method, which was called.
   * @param call The call.
   * @return Some FlowFact, if necessary. Otherwise None.
   */
  protected def createFlowFact(callee: Function, call: LLVMStatement,
                               in: Set[Fact]): Option[FlowFact[Function]]

  /**
   * If a parameter is tainted, the result will also be tainted.
   * We assume that the callee does not call the source method.
   */
  override def callOutsideOfAnalysisContext(statement: LLVMStatement, callee: Function,
                                            successor: LLVMStatement,
                                            in:        Set[Fact]): Set[Fact] = {
    // TODO
    Set.empty
  }

  /**
   * Checks, if a `callee` should be analyzed, i.e. callFlow and returnFlow should create facts.
   * True by default. This method can be overwritten by a subclass.
   *
   * @param callee The callee.
   * @return True, by default.
   */
  protected def relevantCallee(callee: Function): Boolean = true
}