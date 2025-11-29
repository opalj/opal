/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Field
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.immutability.FieldAssignability
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL0FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL1FieldAssignabilityAnalysis
import org.opalj.tac.fpcf.analyses.fieldassignability.EagerL2FieldAssignabilityAnalysis

/**
 * Tests the field assignability analysis
 *
 * @author Tobias Roth
 * @author Maximilian Rüsch
 */
class FieldAssignabilityTests extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability")
    }

    override def createConfig(): Config = ConfigFactory.load("LibraryProject.conf")

    override def init(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>
            import org.opalj.ai.domain
            Set[Class[? <: AnyRef]](classOf[domain.l1.DefaultDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
    }

    var l0: Option[(SomeProject, PropertyStore)] = None
    describe("the org.opalj.fpcf.analyses.L0FieldAssignability is executable") {
        val as = executeAnalyses(
            Set(
                EagerL0FieldAssignabilityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()
        l0 = Some(as.project, as.propertyStore)

        describe("and produces the correct properties") {
            validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
        }
    }

    var l1: Option[(SomeProject, PropertyStore)] = None
    describe("the org.opalj.fpcf.analyses.L1FieldAssignability is executable") {
        val as = executeAnalyses(
            Set(
                EagerL1FieldAssignabilityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()
        l1 = Some(as.project, as.propertyStore)

        describe("and produces the correct properties") {
            validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
        }

        it("and produces properties at least as precise as L0") {
            checkMorePrecise(l0.get._1, l0.get._2, as.project, as.propertyStore)
        }
    }

    describe("the org.opalj.fpcf.analyses.L2FieldAssignability is executable") {
        val as = executeAnalyses(
            Set(
                EagerL2FieldAssignabilityAnalysis,
                LazyInterProceduralEscapeAnalysis,
                LazyReturnValueFreshnessAnalysis,
                LazyFieldLocalityAnalysis,
                EagerFieldAccessInformationAnalysis
            )
        )
        as.propertyStore.shutdown()

        describe("and produces the correct properties") {
            validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
        }

        it("and produces properties at least as precise as L1") {
            checkMorePrecise(l1.get._1, l1.get._2, as.project, as.propertyStore)
        }
    }

    /**
     * Represents a case where the alleged less precise value is not actually less (or equally) precise as the other.
     */
    case class ImpreciseFieldValue(field: Field, lessPrecise: FieldAssignability, morePrecise: FieldAssignability)

    private def checkMorePrecise(
        lessPreciseProject: SomeProject,
        lessPrecisePS:      PropertyStore,
        morePreciseProject: SomeProject,
        morePrecisePS:      PropertyStore
    ): Unit = {
        var impreciseFieldValues: List[ImpreciseFieldValue] = List.empty
        morePreciseProject.allFields.foreach { field =>
            val lessPreciseValue = lessPrecisePS.get(field, FieldAssignability.key).get.asFinal.p
            val morePreciseValue = morePrecisePS.get(field, FieldAssignability.key).get.asFinal.p

            // The less precise value should be "strictly" less precise, meaning it is ordered w.r.t. the precise value
            if (lessPreciseValue.meet(morePreciseValue) ne lessPreciseValue) {
                impreciseFieldValues ::= ImpreciseFieldValue(field, lessPreciseValue, morePreciseValue)
            }
        }

        assert(impreciseFieldValues.isEmpty, s"found precision violations:\n${impreciseFieldValues.mkString("\n")}")
    }
}
