/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.{PropertyBounds, PropertyKey, PropertyStore}
import org.opalj.fpcf.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.ifds.{LLVMStatement, NativeFunction, NativeIFDSAnalysis, NativeIFDSAnalysisScheduler}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.value.Function
import org.opalj.tac.fpcf.properties.Taint

import scala.reflect.ClassTag

class SimpleNativeForwardTaintProblem(p: SomeProject) extends NativeForwardTaintProblem(p) {
    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(NativeFunction, NativeTaintFact)] = Seq.empty
    override val javaPropertyKey: PropertyKey[Taint] = Taint.key

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: NativeFunction): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizesParameter(call: LLVMStatement, in: NativeTaintFact): Boolean = false

    /**
     * Creates a new variable fact for the callee, if the source was called.
     */
    protected def createTaints(callee: Function, call: LLVMStatement): Set[NativeTaintFact] =
        if (callee.name == "source") Set(NativeVariable(call.instruction))
        else Set.empty

    /**
     * Create a FlowFact, if sink is called with a tainted variable.
     * Note, that sink does not accept array parameters. No need to handle them.
     */
    protected def createFlowFact(
        callee: Function,
        call:   LLVMStatement,
        in:     Set[NativeTaintFact]
    ): Option[NativeFlowFact] =
        if (callee.name == "sink" && in.contains(JavaVariable(-2))) Some(NativeFlowFact(Seq(call.function)))
        else None
}

class SimpleNativeForwardTaintAnalysis(project: SomeProject)
    extends NativeIFDSAnalysis(project, new SimpleNativeForwardTaintProblem(project), NativeTaint)

object NativeForwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[NativeTaintFact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeForwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] = NativeTaint
    override val uses: Set[PropertyBounds] = Set() // ++ PropertyBounds.ub(Taint) TODO: we do not use the native taint yet
    override implicit val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}