/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll;

import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.taint_xlang.XlangBackwardFlowPath
import org.opalj.ifds.IFDSFact
import org.opalj.ll.fpcf.analyses.cg.SimpleCallGraphKey
import org.opalj.ll.fpcf.analyses.ifds.taint.{JavaBackwardTaintAnalysisScheduler, NativeBackwardTaintAnalysisScheduler}
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact

import java.net.URL

class CrossLanguageBackwardTaintAnalysisTest extends PropertiesTest {
    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            current => List("./DEVELOPING_OPAL/validate/src/test/resources/llvm/cross_language/taint/TaintTest.ll")
        )
        val llvmProject = p.get(LLVMProjectKey)

        // use all java native functions as entry points for native call graph analysis
        val cgEntryPoints = llvmProject.functions.filter(_.name.startsWith("Java_"))
        p.updateProjectInformationKeyInitializationData(SimpleCallGraphKey)(
            current => cgEntryPoints.toSet
        )

        p.get(RTACallGraphKey)
    }

    describe("CrossLanguageBackwardTaintAnalysisTest") {
        val testContext = executeAnalyses(JavaBackwardTaintAnalysisScheduler, NativeBackwardTaintAnalysisScheduler)
        val project = testContext.project
        val eas = methodsWithAnnotations(project)
            .filter(_._1.classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint_xlang/TaintTest")
            .map {
                case (method, entityString, annotations) =>
                    ((method, new IFDSFact(TaintNullFact)), entityString, annotations)
            }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(XlangBackwardFlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
