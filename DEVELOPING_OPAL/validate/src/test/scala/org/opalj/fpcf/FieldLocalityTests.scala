/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis

class FieldLocalityTests extends PropertiesTest {

    val analyses = Set[FPCFAnalysisScheduler](
        EagerFieldLocalityAnalysis,
        LazyInterProceduralEscapeAnalysis,
        LazyReturnValueFreshnessAnalysis,
        EagerFieldAccessInformationAnalysis
    )

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/field_locality")
    }

    override def init(p: Project[URL]): Unit = {
        p.get(RTACallGraphKey)
    }

    describe("field locality analysis is executed") {
        val as = executeAnalyses(analyses)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            fieldsWithAnnotations(as.project),
            Set("FieldLocality")
        )
    }
}
