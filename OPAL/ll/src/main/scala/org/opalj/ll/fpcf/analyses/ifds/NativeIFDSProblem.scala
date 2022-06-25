/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.PropertyStore
import org.opalj.ifds.Dependees.Getter
import org.opalj.ifds.{AbstractIFDSFact, IFDSProblem}
import org.opalj.ll.LLVMProjectKey

abstract class NativeIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, NativeFunction, LLVMStatement](NativeForwardICFG) {
    final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
    val llvmProject = project.get(LLVMProjectKey)

    override def outsideAnalysisContext(callee: NativeFunction): Option[(LLVMStatement, LLVMStatement, Fact, Getter) ⇒ Set[Fact]] = callee match {
        case LLVMFunction(function) ⇒
            function.basicBlockCount match {
                case 0 ⇒ Some((_: LLVMStatement, _: LLVMStatement, in: Fact, _: Getter) ⇒ Set(in))
                case _ ⇒ None
            }
    }
}
