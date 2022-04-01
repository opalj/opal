/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ifds.IFDSProperty
import org.opalj.ll.fpcf.analyses.ifds.taint.{JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler}
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, NullFact}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MultilingualForwardIFDSTaintAnalysisTests extends AnyFunSpec with Matchers {
    describe("MultilingualForwardTaintAnalysis") {
        it("executes") {
            val project =
                Project(
                    new java.io.File("./DEVELOPING_OPAL/validate/src/test/resources/llvm/multilingual/taint")
                )
            project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
                current ⇒ List("./DEVELOPING_OPAL/validate/src/test/resources/llvm/multilingual/taint/NativeTest.ll")
            )
            project.get(LLVMProjectKey)
            project.get(RTACallGraphKey)
            val manager = project.get(FPCFAnalysesManagerKey)
            val (ps, analyses) = manager.runAll(JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler)
            for (method ← project.allMethodsWithBody) {
                val flows =
                    ps((method, NullFact), JavaForwardTaintAnalysisScheduler.property.key)
                println("---METHOD: "+method.toJava+"  ---")
                for {
                    fact ← flows.ub
                        .asInstanceOf[IFDSProperty[JavaStatement, Fact]]
                        .flows
                        .values
                        .flatten
                        .toSet[Fact]
                } {
                    fact match {
                        case FlowFact(flow) ⇒ println(s"flow: "+flow.map(_.name).mkString(", "))
                        case _              ⇒
                    }
                }
            }
        }
    }
}
