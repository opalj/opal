/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ll

import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.PropertiesTest
import org.opalj.fpcf.properties.taint.XlangBackwardFlowPath
import org.opalj.ifds.IFDSFact
import org.opalj.ll.fpcf.analyses.cg.SimpleNativeCallGraphKey
import org.opalj.ll.fpcf.analyses.ifds.taint.JavaBackwardTaintAnalysisScheduler
import org.opalj.ll.fpcf.analyses.ifds.taint.NativeBackwardTaintAnalysisScheduler
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.ifds.taint.TaintNullFact

import java.net.URL

/**
 * Tests the cross language backwards IFDS based taint analysis
 *
 * @author Nicolas Gross
 */
class CrossLanguageBackwardTaintAnalysisTest extends PropertiesTest {
    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(LLVMProjectKey)(
            _ => List("./DEVELOPING_OPAL/validateCross/src/test/resources/llvm/cross_language/taint/TaintTest.ll")
        )
        val llvmProject = p.get(LLVMProjectKey)

        // use all java native functions as entry points for native call graph analysis
        val cgEntryPoints = llvmProject.functions.filter(_.name.startsWith("Java_"))
        p.updateProjectInformationKeyInitializationData(SimpleNativeCallGraphKey)(
            _ => cgEntryPoints.toSet
        )

        p.get(RTACallGraphKey)
    }

    describe("CrossLanguageBackwardTaintAnalysisTest") {
        val testContext = executeAnalyses(JavaBackwardTaintAnalysisScheduler, NativeBackwardTaintAnalysisScheduler)
        val project = testContext.project
        val eas = methodsWithAnnotations(project)
            .filter(_._1.classFile.thisType == ObjectType("org/opalj/fpcf/fixtures/taint_xlang/TaintTest"))
            .map {
                case (method, entityString, annotations) =>
                    ((method, new IFDSFact(TaintNullFact)), entityString, annotations)
            }
        testContext.propertyStore.shutdown()
        validateProperties(testContext, eas, Set(XlangBackwardFlowPath.PROPERTY_VALIDATOR_KEY))
    }
}
