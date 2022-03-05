/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds

import org.opalj.br.analyses.{SomeProject}
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.{PropertyStore}
import org.opalj.ll.LLVMProjectKey
import org.opalj.tac.fpcf.analyses.ifds.{AbstractIFDSFact, IFDSProblem}
import org.opalj.ll.llvm.Function

abstract class NativeIFDSProblem[Fact <: AbstractIFDSFact](project: SomeProject) extends IFDSProblem[Fact, Function, LLVMStatement](project) {
        final implicit val propertyStore: PropertyStore = project.get(PropertyStoreKey)
        val llvmProject = project.get(LLVMProjectKey)

        /**
         * Checks, if a callee is inside this analysis' context.
         * If not, `callOutsideOfAnalysisContext` is called instead of analyzing the callee.
         * By default, native methods are not inside the analysis context.
         *
         * @param callee The callee.
         * @return True, if the callee is inside the analysis context.
         */
        override def insideAnalysisContext(callee: Function): Boolean = true

        override def getCallees(
        statement: LLVMStatement,
        caller:    Function
        ): Iterator[Function] = ???
}
