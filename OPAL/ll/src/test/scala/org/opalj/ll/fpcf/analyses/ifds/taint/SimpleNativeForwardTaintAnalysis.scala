/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Callable
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.NativeFunction
import org.opalj.ll.fpcf.analyses.ifds.NativeIFDSAnalysis
import org.opalj.ll.fpcf.analyses.ifds.NativeIFDSAnalysisScheduler
import org.opalj.tac.fpcf.properties.Taint

/**
 * Class to solve a native forward taint problem.
 *
 * @author Marc Clement
 */
class SimpleNativeForwardTaintProblem(p: SomeProject) extends NativeForwardTaintProblem(p) {
    /**
     * The analysis starts with all public methods in TaintAnalysisTestClass.
     */
    override val entryPoints: Seq[(NativeFunction, IFDSFact[NativeTaintFact, LLVMStatement])] = Seq.empty
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

    override def createFlowFactAtExit(callee: NativeFunction, in: NativeTaintFact,
                                      unbCallChain: Seq[Callable]): Option[NativeTaintFact] = None
}

class SimpleNativeForwardTaintAnalysis(project: SomeProject)
    extends NativeIFDSAnalysis(project, new SimpleNativeForwardTaintProblem(project), NativeTaint)

object NativeForwardTaintAnalysisScheduler extends NativeIFDSAnalysisScheduler[NativeTaintFact] {
    override def init(p: SomeProject, ps: PropertyStore) = new SimpleNativeForwardTaintAnalysis(p)
    override def property: IFDSPropertyMetaInformation[LLVMStatement, NativeTaintFact] = NativeTaint
    override val uses: Set[PropertyBounds] = Set() // ++ PropertyBounds.ub(Taint) TODO: we do not use the native taint yet
}