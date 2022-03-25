/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.{PropertyBounds, PropertyStore}
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.ifds.{NativeIFDSAnalysis, LLVMFunction, LLVMStatement, NativeIFDSAnalysisScheduler}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.Function

class SimpleNativeForwardTaintProblem(p: SomeProject) extends NativeForwardTaintProblem(p) {
    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(Function, NativeFact)] = Seq.empty

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: Function): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizesParameter(call: LLVMStatement, in: NativeFact): Boolean = false

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    protected def createTaints(callee: Function, call: LLVMStatement): Set[NativeFact] =
        if (callee.name == "source") Set(NativeVariable(call))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    protected def createFlowFact(
        callee: Function,
        call:   LLVMStatement,
        in:     Set[NativeFact]
    ): Option[NativeFlowFact] =
        if (callee.name == "sink" && in.contains(NativeVariable(-2))) Some(NativeFlowFact(Seq(LLVMFunction(call.function))))
        else None
}

class SimpleNativeForwardTaintAnalysis(implicit project: SomeProject)
    extends NativeIFDSAnalysis(project, new SimpleNativeForwardTaintProblem(project), NativeTaint)

object NativeForwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[NativeFact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeForwardTaintAnalysis()(p)
    override def property: IFDSPropertyMetaInformation[LLVMStatement, NativeFact] = NativeTaint
    override val uses: Set[PropertyBounds] = Set() // ++ PropertyBounds.ub(Taint) TODO: we do not use the native taint yet
}