/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import com.typesafe.config.ConfigValueFactory
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ifds
import org.opalj.ifds.IFDSProperty
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.taint.{JavaForwardTaintAnalysisScheduler, NativeFact, NativeForwardTaintAnalysisScheduler, NativeNullFact}
import org.opalj.ll.llvm.value.Function
import org.opalj.log.GlobalLogContext
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{Fact, FlowFact, NullFact}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class MultilingualForwardIFDSTaintAnalysisTests extends AnyFunSpec with Matchers {
    describe("MultilingualForwardTaintAnalysis") {
        implicit val config = BaseConfig.withValue(ifds.ConfigKeyPrefix+"debug", ConfigValueFactory.fromAnyRef(true))
        val project =
            Project(
                new java.io.File("./DEVELOPING_OPAL/validate/src/test/resources/llvm/multilingual/taint"),
                GlobalLogContext,
                config
            )

        project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            current ⇒ List("./DEVELOPING_OPAL/validate/src/test/resources/llvm/multilingual/taint/TaintTest.ll")
        )
        project.get(LLVMProjectKey)
        project.get(RTACallGraphKey)
        val manager = project.get(FPCFAnalysesManagerKey)
        val (ps, analyses) = manager.runAll(JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler)
        for (method ← project.allMethodsWithBody) {
            val flows =
                ps((method, NullFact), JavaForwardTaintAnalysisScheduler.property.key)
            println("---METHOD: "+method.toJava+"  ---")
            val flowFacts = flows.ub
                .asInstanceOf[IFDSProperty[JavaStatement, Fact]]
                .flows
                .values
                .flatten
                .toSet[Fact]
                .flatMap {
                    case FlowFact(flow) ⇒ Some(flow)
                    case _              ⇒ None
                }
            for (flow ← flowFacts)
                println(s"flow: "+flow.map(_.name).mkString(", "))
            if (method.name.contains("no_flow")) {
                it(s"${method.name} has no flow") {
                    assert(flowFacts.isEmpty)
                }
            } else if (method.name.contains("flow")) {
                it(s"${method.name} has some flow") {
                    assert(!flowFacts.isEmpty)
                }
            }
        }

        val function: Function = project.get(LLVMProjectKey).function("Java_TaintTest_native_1array_1tainted").get
        val debugData = ps((function, NativeNullFact), NativeForwardTaintAnalysisScheduler.property.key).ub.asInstanceOf[IFDSProperty[LLVMStatement, NativeFact]].debugData
        for {
            bb ← function.basicBlocks
            instruction ← bb.instructions
        } {
            for (fact ← debugData.getOrElse(LLVMStatement(instruction), Set.empty))
                println("\t"+fact)
            println(instruction.repr)
        }
    }
}
