/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.{SomeProject}
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ll.fpcf.analyses.ifds.{ForwardNativeIFDSAnalysis, LLVMStatement, NativeIFDSAnalysisScheduler}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.Function
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, Variable}
import org.opalj.tac.fpcf.properties.{IFDSPropertyMetaInformation}

class SimpleNativeForwardTaintProblem(p: SomeProject) extends NativeForwardTaintProblem(p) {
  /**
   * The analysis starts with all public methods in TaintAnalysisTestClass.
   */
  override val entryPoints: Seq[(Function, Fact)] = Seq.empty

  /**
   * The sanitize method is a sanitizer.
   */
  override protected def sanitizesReturnValue(callee: Function): Boolean =
    callee.name == "sanitize"

  /**
   * We do not sanitize parameters.
   */
  override protected def sanitizeParameters(call: LLVMStatement, in: Set[Fact]): Set[Fact] = Set.empty

  /**
   * Creates a new variable fact for the callee, if the source was called.
   */
  override protected def createTaints(callee: Function, call: LLVMStatement): Set[Fact] =
    if (callee.name == "source") Set(Variable(call.index))
    else Set.empty

  /**
   * Create a FlowFact, if sink is called with a tainted variable.
   * Note, that sink does not accept array parameters. No need to handle them.
   */
  override protected def createFlowFact(
                                         callee: Function,
                                         call:   LLVMStatement,
                                         in:     Set[Fact]
                                       ): Option[FlowFact[NativeTaint.Callable]] =
    if (callee.name == "sink" && in.contains(Variable(-2))) Some(FlowFact(Seq(call.function)))
    else None
}

class SimpleNativeForwardTaintAnalysis(implicit val project: SomeProject)
  extends ForwardNativeIFDSAnalysis(new SimpleNativeForwardTaintProblem(project), NativeTaint)

object NativeForwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[Fact] {
  override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeForwardTaintAnalysis()(p)
  override def property: IFDSPropertyMetaInformation[LLVMStatement, Fact] = NativeTaint
  override val uses: Set[PropertyBounds] = super.uses // ++ PropertyBounds.ub(Taint) TODO: we do not use the native taint yet
}