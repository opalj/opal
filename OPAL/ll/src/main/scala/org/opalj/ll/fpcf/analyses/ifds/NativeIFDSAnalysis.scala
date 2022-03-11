/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.{BasicBlock, Function, Instruction}
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact, IFDSProblem}
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class NativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        ifdsProblem: IFDSProblem[IFDSFact, Function, LLVMStatement],
        icfg:        ICFG[IFDSFact, Function, LLVMStatement, BasicBlock],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
)
    extends IFDSAnalysis[IFDSFact, Function, LLVMStatement, BasicBlock](ifdsProblem, icfg, propertyKey)

/**
 * A statement that is passed to the concrete analysis.
 *
 * @param instruction The LLVM instruction.
 */
case class LLVMStatement(instruction: Instruction) extends Statement[BasicBlock] {
    def function(): Function = instruction.function
    def basicBlock(): BasicBlock = instruction.parent
    override def node(): BasicBlock = basicBlock
}

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact] extends IFDSAnalysisScheduler[IFDSFact, Function, LLVMStatement, BasicBlock] {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
}
