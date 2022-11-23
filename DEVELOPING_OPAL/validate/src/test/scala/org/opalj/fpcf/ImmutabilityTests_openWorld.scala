/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL
import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.escape.LazySimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerClassImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerL0FieldImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.EagerTypeImmutabilityAnalysis
import org.opalj.tac.fpcf.analyses.immutability.field_assignability.EagerL2FieldAssignabilityAnalysis
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
/**
 * Tests the immutability analyses with an open world assumption
 *
 * @author Tobias Roth
 */
class ImmutabilityTests_openWorld extends PropertiesTest {

    override def withRT = true

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/immutability/open_world")
    }

    override def createConfig(): Config = ConfigFactory.load("LibraryProject.conf")

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>

            Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        }

        p.get(RTACallGraphKey)
    }

    describe("run the immutability analysis with an open world assumption (without type provider") {

        val as = executeAnalyses(
            Set(
                EagerL2FieldAssignabilityAnalysis,
                EagerL0FieldImmutabilityAnalysis,
                EagerClassImmutabilityAnalysis,
                EagerTypeImmutabilityAnalysis,
                LazyStaticDataUsageAnalysis,
                LazyL0CompileTimeConstancyAnalysis,
                LazySimpleEscapeAnalysis
            )
        )

        as.propertyStore.shutdown()

        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldAssignability"))
        validateProperties(as, fieldsWithAnnotations(as.project), Set("FieldImmutability"))
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp => (tp._1.thisType, tp._2, tp._3)),
            Set("ClassImmutability")
        )
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp => (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability")
        )
    }
}
