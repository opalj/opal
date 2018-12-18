/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.fpcf.analyses.EagerFieldLocalityAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIResultAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer

class FieldLocalityTests extends PropertiesTest {

    val lazyAnalysisSchedulers = Set[FPCFLazyLikeAnalysisScheduler](
        LazyL0BaseAIResultAnalysis,
        TACAITransformer, //LazyL0TACAIAnalysis,
        LazyInterProceduralEscapeAnalysis,
        LazyVirtualCallAggregatingEscapeAnalysis,
        LazyVirtualReturnValueFreshnessAnalysis,
        LazyReturnValueFreshnessAnalysis
    )

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(
            AIDomainFactoryKey,
            (_: Option[Set[Class[_ <: AnyRef]]]) ⇒
                Set(classOf[DefaultDomainWithCFGAndDefUse[URL]]): Set[Class[_ <: AnyRef]]
        )
    }

    describe("field locality analysis is executed") {
        val as = executeAnalyses(Set(EagerFieldLocalityAnalysis), lazyAnalysisSchedulers)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            fieldsWithAnnotations(as.project),
            Set("FieldLocality")
        )
    }
}
