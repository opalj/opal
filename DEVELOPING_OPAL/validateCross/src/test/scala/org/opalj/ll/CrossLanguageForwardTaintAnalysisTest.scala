/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.bi.TestResources
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.ifds
import org.opalj.ifds.IFDSFact
import org.opalj.ifds.IFDSProperty
import org.opalj.ll.fpcf.analyses.ifds.LLVMFunction
import org.opalj.ll.fpcf.analyses.ifds.LLVMStatement
import org.opalj.ll.fpcf.analyses.ifds.taint.JavaForwardTaintAnalysis
import org.opalj.ll.fpcf.analyses.ifds.taint.JavaForwardTaintAnalysisScheduler
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeForwardTaintAnalysisScheduler
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeTaintFact
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeTaintNullFact
import org.opalj.ll.llvm.value.Function
import org.opalj.log.GlobalLogContext
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.FlowFact
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintFact
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests the cross language forward IFDS based taint analysis
 *
 * @author Marc Clement
 */
class CrossLanguageForwardTaintAnalysisTest extends AnyFunSpec with Matchers {
    describe("CrossLanguageForwardTaintAnalysis") {
        implicit val config: Config = BaseConfig.withValue(
            ifds.ConfigKeyPrefix+"debug",
            ConfigValueFactory.fromAnyRef(true)
        )
        val project =
            Project(
                TestResources.locateTestResources(
                    "/llvm/cross_language/taint/",
                    "DEVELOPING_OPAL/validateCross"
                ),
                GlobalLogContext,
                config
            )

        project.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            _ => List(TestResources.locateTestResources(
                "/llvm/cross_language/taint/TaintTest.ll",
                "DEVELOPING_OPAL/validateCross"
            ).toString)
        )
        project.get(LLVMProjectKey)
        project.get(RTACallGraphKey)
        val manager = project.get(FPCFAnalysesManagerKey)
        val (ps, analyses) = manager.runAll(JavaForwardTaintAnalysisScheduler, NativeForwardTaintAnalysisScheduler)
        for ((method, fact) <- analyses.head._2.asInstanceOf[JavaForwardTaintAnalysis].ifdsProblem.entryPoints) {
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
                    assert(flowFacts.nonEmpty)
                }
            }
        }

        val function: Function = project.get(LLVMProjectKey).function("Java_org_opalj_fpcf_fixtures_taint_xlang_TaintTest_native_1array_1tainted").get
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
