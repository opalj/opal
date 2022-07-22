/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{ProjectInformationKeys, SomeProject}
import org.opalj.ifds.{AbstractIFDSFact, IFDSAnalysis, IFDSAnalysisScheduler, IFDSProblem, IFDSPropertyMetaInformation}
import org.opalj.ll.LLVMProjectKey

/**
 *
 * @param ifdsProblem
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and the lower bound for the IFDSProperty.
 * @tparam IFDSFact
 */
class NativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        project:     SomeProject,
        ifdsProblem: IFDSProblem[IFDSFact, NativeFunction, LLVMStatement],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
)
    extends IFDSAnalysis[IFDSFact, NativeFunction, LLVMStatement]()(project, ifdsProblem, propertyKey)

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact] extends IFDSAnalysisScheduler[IFDSFact, NativeFunction, LLVMStatement] {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
}
