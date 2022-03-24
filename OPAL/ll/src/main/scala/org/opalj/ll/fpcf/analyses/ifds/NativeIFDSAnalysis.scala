/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSProblem, Statement}
import org.opalj.ifds.{AbstractIFDSFact, IFDSPropertyMetaInformation}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.{BasicBlock, Function, Instruction}

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class NativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        project:     SomeProject,
        ifdsProblem: IFDSProblem[IFDSFact, Function, LLVMStatement],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
)
    extends IFDSAnalysis[IFDSFact, Function, LLVMStatement]()(project, ifdsProblem, propertyKey)

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) extends Statement[Function, BasicBlock] {
    def function(): Function = instruction.function
    def basicBlock(): BasicBlock = instruction.parent
    override def node(): BasicBlock = basicBlock
    override def callable(): Function = function
}

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact] extends IFDSAnalysisScheduler[IFDSFact, Function, LLVMStatement] {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
}
