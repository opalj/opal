/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ll.fpcf.analyses.EagerSimplePurityAnalysis
import org.opalj.ll.fpcf.analyses.Impure
import org.opalj.ll.fpcf.analyses.Pure
import org.opalj.ll.llvm.value.Function
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class SimpleLLVMPurityTests extends AnyFunSpec with Matchers {
    describe("SimplePurityAnalysis") {
        it("executes") {
            val project = Project(Iterable.empty)
            project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
                current => List("./DEVELOPING_OPAL/validate/src/test/resources/llvm/purity.ll")
            )
            val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(EagerSimplePurityAnalysis)

            val impureFunctionNames = propertyStore
                .finalEntities(Impure)
                .asInstanceOf[Iterator[Function]]
                .map(_.name)
                .toList
            impureFunctionNames should contain("impure_function")
            val pureFunctionNames = propertyStore
                .finalEntities(Pure)
                .asInstanceOf[Iterator[Function]]
                .map(_.name)
                .toList
            pureFunctionNames should contain("pure_function")
        }
    }
}
