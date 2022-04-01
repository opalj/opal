/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.ifds.{IFDSAnalysis, IFDSAnalysisScheduler, IFDSProblem, Statement}
import org.opalj.ifds.{AbstractIFDSFact, IFDSPropertyMetaInformation}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.value.{BasicBlock, Instruction}
import org.opalj.ll.llvm.value

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class NativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        project:     SomeProject,
        ifdsProblem: IFDSProblem[IFDSFact, value.Function, LLVMStatement],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
)
    extends IFDSAnalysis[IFDSFact, value.Function, LLVMStatement]()(project, ifdsProblem, propertyKey)

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) extends Statement[value.Function, BasicBlock] {
    def function: value.Function = instruction.function
    def basicBlock: BasicBlock = instruction.parent
    override def node: BasicBlock = basicBlock
    override def callable: value.Function = function
    override def toString: String = s"${function.name}\n\t${instruction}\n\t${function}"
}

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact] extends IFDSAnalysisScheduler[IFDSFact, value.Function, LLVMStatement] {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
}
