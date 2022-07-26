/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import java.net.URL

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.analyses.escape.EagerInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis

/**
 * Tests if the escape properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class EscapeAnalysisTests extends PropertiesTest {

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/escape")
    }

    override def init(p: Project[URL]): Unit = {
        val performInvocationsDomain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(performInvocationsDomain)
            case Some(requirements) => requirements + performInvocationsDomain
        }

        p.get(RTACallGraphKey)
    }

    private[this] def mapEntities(
        p: Project[URL], es: Iterable[(Entity, String => String, Iterable[AnnotationLike])]
    ): Iterable[(Entity, String => String, Iterable[AnnotationLike])] = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val simpleContexts = p.get(SimpleContextsKey)
        es.map { tuple =>
            (
                tuple._1 match {
                    case ds: DefinitionSite         => (simpleContexts(declaredMethods(ds.method)), ds)
                    case fp: VirtualFormalParameter => (simpleContexts(fp.method), fp)
                },
                tuple._2,
                tuple._3
            )
        }
    }

    describe("no analysis is scheduled") {
        val as = executeAnalyses(Set.empty)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            mapEntities(
                as.project,
                allocationSitesWithAnnotations(as.project) ++
                    explicitFormalParametersWithAnnotations(as.project)
            ),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.fpcf.analyses.escape.SimpleEscapeAnalysis is executed") {
        val as = executeAnalyses(EagerSimpleEscapeAnalysis)
        as.propertyStore.shutdown()
        validateProperties(
            as,
            mapEntities(
                as.project,
                allocationSitesWithAnnotations(as.project) ++
                    explicitFormalParametersWithAnnotations(as.project)
            ),
            Set("EscapeProperty")
        )
    }

    describe("the org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis is executed") {
        val as = executeAnalyses(EagerInterProceduralEscapeAnalysis)

        as.propertyStore.shutdown()
        validateProperties(
            as,
            mapEntities(
                as.project,
                allocationSitesWithAnnotations(as.project) ++
                    explicitFormalParametersWithAnnotations(as.project)
            ),
            Set("EscapeProperty")
        )
    }

}
