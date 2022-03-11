/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSFact
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * An IFDS analysis, which analyzes the code in the control flow direction.
 *
 * @author Mario Trageser
 */

class ForwardNativeIFDSAnalysis[IFDSFact <: AbstractIFDSFact](
        ifdsProblem: NativeIFDSProblem[IFDSFact],
        propertyKey: IFDSPropertyMetaInformation[LLVMStatement, IFDSFact]
) extends NativeIFDSAnalysis[IFDSFact](
    ifdsProblem, new NativeForwardICFG[IFDSFact], propertyKey
)
