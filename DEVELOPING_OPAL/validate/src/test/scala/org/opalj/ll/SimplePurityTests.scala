/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable.List

class SimplePurityTests extends AnyFunSpec with Matchers {
    describe("SimplePurityAnalysis") {
        it("executes") {
            val project = Project(Traversable.empty)
            project.updateProjectInformationKeyInitializationData(LLVMModulesKey)(current =>
              current.getOrElse(Iterable.empty) ++ List("./OPAL/ll/src/test/resources/org/opalj/ll/test.ll")
            )
            val (propertyStore, _) = project.get(FPCFAnalysesManagerKey).runAll(EagerSimplePurityAnalysis)

            assert(propertyStore.finalEntities(Pure).size == 0)
            assert(propertyStore.finalEntities(Impure).size == 4)
        }
    }
}
