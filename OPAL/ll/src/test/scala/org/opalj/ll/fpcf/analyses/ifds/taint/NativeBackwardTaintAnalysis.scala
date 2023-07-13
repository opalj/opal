/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.cg.SimpleNativeCallGraphKey
import org.opalj.ll.fpcf.analyses.ifds.LLVMFunction
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.NativeFunction
import org.opalj.ll.fpcf.analyses.ifds.NativeIFDSAnalysis
import org.opalj.ll.fpcf.analyses.ifds.NativeIFDSAnalysisScheduler
import org.opalj.ll.llvm.value.Call
import org.opalj.tac.fpcf.properties.Taint

/**
 * This is a simple IFDS based backward taint analysis
 *
 * @author Nicolas Gross
 */
class SimpleNativeBackwardTaintProblem(p: SomeProject) extends NativeBackwardTaintProblem(p) {

    override val javaPropertyKey: PropertyKey[Taint] = Taint.key

    /**
     * The analysis starts with the sink function.
     */
    override val entryPoints: Seq[(NativeFunction, IFDSFact[NativeTaintFact, LLVMStatement])] = {
        val sinkFunc = llvmProject.function("sink")
        if (sinkFunc.isDefined)
            Seq((LLVMFunction(sinkFunc.get), new IFDSFact(NativeVariable(sinkFunc.get.argument(0)))))
        else Seq.empty
    }

    /**
     * The sanitize method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: NativeFunction): Boolean =
        callee.name == "sanitize"

    /**
     * We do not sanitize parameters.
     */
    override protected def sanitizesParameter(call: LLVMStatement, in: NativeTaintFact): Boolean =
        false

    override protected def createFlowFactAtCall(
        call:         LLVMStatement,
        in:           NativeTaintFact,
        unbCallChain: Seq[Callable]
    ): Option[NativeTaintFact] = {
        // create flow facts if callee is source or sink
        val callInstr = call.instruction.asInstanceOf[Call]
        val callees = icfg.resolveCallee(callInstr)
        if (callees.exists(_.name == "source")) in match {
            // create flow fact if source is reached with tainted value
            case NativeVariable(value) if value == call.instruction && !unbCallChain.contains(call.callable) =>
                Some(NativeFlowFact(unbCallChain.prepended(call.callable)))
            case _ => None
        }
        else None
    }

    override def createFlowFactAtExit(
        callee:       NativeFunction,
        in:           NativeTaintFact,
        unbCallChain: Seq[Callable]
    ): Option[NativeTaintFact] = None
}

class SimpleNativeBackwardTaintAnalysis(project: SomeProject)
    extends NativeIFDSAnalysis(project, new SimpleNativeBackwardTaintProblem(project), NativeTaint)

object NativeBackwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[NativeTaintFact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeBackwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] = NativeTaint
    override val uses: Set[PropertyBounds] = Set()
    override def requiredProjectInformation: ProjectInformationKeys =
        SimpleNativeCallGraphKey +: super.requiredProjectInformation
}
