/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.fpcf.{PropertyBounds, PropertyKey, PropertyStore}
import org.opalj.ifds.{Callable, IFDSFact, IFDSPropertyMetaInformation}
import org.opalj.ll.fpcf.analyses.cg.SimpleCallGraphKey
import org.opalj.ll.fpcf.analyses.ifds.{LLVMStatement, NativeFunction, NativeIFDSAnalysis, NativeIFDSAnalysisScheduler}
import org.opalj.ll.fpcf.properties.NativeTaint
import org.opalj.ll.llvm.value.Call
import org.opalj.tac.fpcf.properties.Taint

class SimpleNativeBackwardTaintProblem(p: SomeProject) extends NativeBackwardTaintProblem(p) {

    override val javaPropertyKey: PropertyKey[Taint] = Taint.key
    override val entryPoints: Seq[(NativeFunction, IFDSFact[NativeTaintFact, LLVMStatement])] = Seq.empty

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: NativeFunction): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizesParameter(call: LLVMStatement, in: NativeTaintFact): Boolean = false

    override protected def createFlowFactAtCall(call: LLVMStatement, in: NativeTaintFact,
                                                callChain: Seq[Callable]): Option[NativeTaintFact] = {
        // create flow facts if callee is source or sink
        val callInstr = call.instruction.asInstanceOf[Call]
        val callees = icfg.resolveCallee(callInstr)
        if (callees.exists(_.name == "sink")) in match {
            // taint variable that is put into sink
            case NativeTaintNullFact => Some(NativeVariable(callInstr.argument(0).get))
            case _ => None
        } else if (callees.exists(_.name == "source")) in match {
            // create flow fact if source is reached with tainted value
            case NativeVariable(value) if value == call.instruction && !callChain.contains(call.callable) =>
                Some(NativeFlowFact(callChain.prepended(call.callable)))
            case _ => None
        } else None
    }

    override def createFlowFactAtExit(callee: NativeFunction, in: NativeTaintFact,
                                      unbCallChain: Seq[Callable]): Option[NativeTaintFact] = None
}

class SimpleNativeBackwardTaintAnalysis(project: SomeProject)
    extends NativeIFDSAnalysis(project, new SimpleNativeBackwardTaintProblem(project), NativeTaint)

object NativeBackwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[NativeTaintFact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeBackwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] = NativeTaint
    override val uses: Set[PropertyBounds] = Set()
    override def requiredProjectInformation: ProjectInformationKeys = SimpleCallGraphKey +: super.requiredProjectInformation
}
