/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.bi.TestResources
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.Pure
import org.opalj.ll.fpcf.analyses.EagerSimplePurityAnalysis
import org.opalj.ll.llvm.value.Function
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class LLVMPurityTests extends AnyFunSpec with Matchers {
    describe("SimplePurityAnalysis") {
        it("executes") {
            val project = Project(Iterable.empty)
            project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
                _ => List(TestResources.locateTestResources("/llvm/purity.ll", "ll").getAbsolutePath)
            )
            val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(EagerSimplePurityAnalysis)

            val impureFunctionNames = propertyStore
                .finalEntities(ImpureByAnalysis)
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
