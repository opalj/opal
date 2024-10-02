/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj
package ll
package fpcf
package analyses
package ifds

import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.IFDSAnalysis
import org.opalj.ifds.IFDSAnalysisScheduler
import org.opalj.ifds.IFDSProblem
import org.opalj.ifds.IFDSPropertyMetaInformation
import org.opalj.ll.LLVMProjectKey

/**
 * Marker class for LLVM - Java cross language IFDS analyses, that sets the corresponding types
 *
 * @param ifdsProblem the actual IFDS Problem that represents the analysis calculation
 * @param propertyKey Provides the concrete property key that must be unique for every distinct concrete analysis and
 *                    the lower bound for the IFDSProperty.
 * @tparam IFDSFact The facts that are computed by the analysis
 *
 * @author Marc Clement
 */
abstract class NativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        project:     SomeProject,
        ifdsProblem: IFDSProblem[IFDSFact, NativeFunction, LLVMStatement],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
) extends IFDSAnalysis[IFDSFact, NativeFunction, LLVMStatement](project, ifdsProblem, propertyKey)

abstract class NativeIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact]
    extends IFDSAnalysisScheduler[IFDSFact, NativeFunction, LLVMStatement] {
    override def requiredProjectInformation: ProjectInformationKeys = Seq(LLVMProjectKey)
}
