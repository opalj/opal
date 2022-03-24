/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.{AbstractIFDSFact, IFDSProblem}
import org.opalj.ll.LLVMProjectKey
import org.opalj.ll.llvm.Function

abstract class NativeIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, Function, LLVMStatement](new NativeForwardICFG[Fact]) {
    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    val llvmProject = project.get(LLVMProjectKey)

    override def outsideAnalysisContext(callee: Function): Option[OutsideAnalysisContextHandler] = None
}
