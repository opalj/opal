/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL2FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.LazyL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.purity.SystemOutLoggingAllExceptionRater

/**
 * Tests the field assignability analysis
 *
 * @author Tobias Roth
 */
class FieldAssignabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability")
    }

    override def createConfig(): Config = ConfigFactory.load("LibraryProject.conf")

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            import org.opalj.ai.domain.l1
            Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)

    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
    }
    L2PurityAnalysis.setRater(Some(SystemOutLoggingAllExceptionRater))

    describe("the org.opalj.fpcf.analyses.L0FieldAssignability is executed") {

        val as = executeAnalyses(
            Set(
                EagerL0FieldAssignabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL2PurityAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
    }

    describe("the org.opalj.fpcf.analyses.L1FieldAssignability is executed") {

        val as = executeAnalyses(
            Set(
                EagerL1FieldAssignabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL2PurityAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
    }

    describe("the org.opalj.fpcf.analyses.L2FieldAssignability is executed") {

        val as = executeAnalyses(
            Set(
                EagerL2FieldAssignabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL2PurityAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
    }
}
