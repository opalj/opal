/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll

import com.typesafe.config.ConfigValueFactory
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ifds
import org.opalj.ifds.{IFDSFact, IFDSProperty}
import org.opalj.ll.fpcf.analyses.ifds.{LLVMFunction, LLVMStatement}
import org.opalj.ll.fpcf.analyses.ifds.taint.{JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler, NativeTaintFact, NativeTaintNullFact}
import org.opalj.ll.fpcf.analyses.ifds.taint.SimpleJavaForwardTaintAnalysis
import org.opalj.ll.llvm.value.Function
import org.opalj.log.GlobalLogContext
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{FlowFact, TaintFact}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class CrossLanguageForwardIFDSTaintAnalysisTests extends AnyFunSpec with Matchers {
    describe("CrossLanguageForwardTaintAnalysis") {
        implicit val config = BaseConfig.withValue(ifds.ConfigKeyPrefix+"debug", ConfigValueFactory.fromAnyRef(true))
        val project =
            Project(
                new java.io.File("./DEVELOPING_OPAL/validate/src/test/resources/llvm/cross_language/taint"),
                GlobalLogContext,
                config
            )

        project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            current => List("./DEVELOPING_OPAL/validate/src/test/resources/llvm/cross_language/taint/TaintTest.ll")
        )
        project.get(LLVMProjectKey)
        project.get(RTACallGraphKey)
        val manager = project.get(FPCFAnalysesManagerKey)
        val (ps, analyses) = manager.runAll(JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler)
        for ((method, fact) <- analyses.head._2.asInstanceOf[SimpleJavaForwardTaintAnalysis].ifdsProblem.entryPoints) {
            val flows =
                ps((method, fact), JavaForwardTaintAnalysisScheduler.property.key)
            println("---METHOD: "+method.toJava+"  ---")
            val flowFacts = flows.ub
                .asInstanceOf[IFDSProperty[JavaStatement, TaintFact]]
                .flows
                .values
                .flatten
                .toSet[TaintFact]
                .flatMap {
                    case FlowFact(flow) => Some(flow)
                    case _              => None
                }
            for (flow <- flowFacts)
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
        val debugData = ps((LLVMFunction(function), new IFDSFact(NativeTaintNullFact)), NativeForwardTaintAnalysisScheduler.property.key).ub.asInstanceOf[IFDSProperty[LLVMStatement, NativeTaintFact]].debugData
        for {
            bb <- function.basicBlocks
            instruction <- bb.instructions
        } {
            for (fact <- debugData.getOrElse(LLVMStatement(instruction), Set.empty))
                println("\t"+fact)
            println(instruction.repr)
        }
    }
}
