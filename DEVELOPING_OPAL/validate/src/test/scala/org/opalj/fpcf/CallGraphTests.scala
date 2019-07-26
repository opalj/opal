/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.fpcf.properties.callgraph.TypePropagationVariant
import org.opalj.tac.fpcf.analyses.cg.CHACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.InstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.rta.RTACallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.CTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.FTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.MTASetEntitySelector
import org.opalj.tac.fpcf.analyses.cg.xta.PropagationBasedCallGraphAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.SimpleInstantiatedTypesAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.TypePropagationAnalysisScheduler
import org.opalj.tac.fpcf.analyses.cg.xta.XTASetEntitySelector

import scala.collection.JavaConverters._

/**
 * Tests if the computed call graph contains (at least!) the expected call edges.
 *
 * @author Andreas Bauer
 */
class CallGraphTests extends PropertiesTest {

    //override val withRT = true

    override def createConfig(): Config = {
        val baseConfig = super.createConfig()
        // For these tests, we want to restrict entry points to "main" methods.
        // Also, no types should be instantiated by default.
        baseConfig.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationEntryPointsFinder")
        ).withValue(
                InitialInstantiatedTypesKey.ConfigKeyPrefix+"analysis",
                ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ApplicationInstantiatedTypesFinder")
            )

        //configuredEntryPoint(baseConfig, "org/opalj/fpcf/fixtures/callgraph/xta/FieldFlows", "main")
    }

    // for testing
    // TODO AB debug stuff, remove later
    def configuredEntryPoint(cfg: Config, declClass: String, name: String): Config = {
        val ep = ConfigValueFactory.fromMap(Map("declaringClass" -> declClass, "name" -> name).asJava)
        cfg.withValue(
            InitialEntryPointsKey.ConfigKeyPrefix+"analysis",
            ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder")
        ).withValue(
                InitialEntryPointsKey.ConfigKeyPrefix+"entryPoints",
                ConfigValueFactory.fromIterable(Seq(ep).asJava)
            )
    }

    describe("the RTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                InstantiatedTypesAnalysisScheduler,
                RTACallGraphAnalysisScheduler
            )
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }

    describe("the CHA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(CHACallGraphAnalysisScheduler)
        )
        as.propertyStore.shutdown()
        validateProperties(
            as,
            declaredMethodsWithAnnotations(as.project),
            Set("Callees")
        )
    }

    describe("the XTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                new SimpleInstantiatedTypesAnalysisScheduler(XTASetEntitySelector),
                PropagationBasedCallGraphAnalysisScheduler,
                new TypePropagationAnalysisScheduler(XTASetEntitySelector)
            )
        )
        as.propertyStore.shutdown()
        // We need to manually store which variant was executed. Otherwise, there is no good way
        // to get this information in the property matcher.
        as.propertyStore.getOrCreateInformation(
            TypePropagationVariant.id, TypePropagationVariant.XTA)

        validateProperties(
            as,
            classFilesWithAnnotations(as.project) ++
                declaredMethodsWithAnnotations(as.project) ++
                fieldsWithAnnotations(as.project),
            Set("AvailableTypes")
        )
    }

    describe("the MTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                new SimpleInstantiatedTypesAnalysisScheduler(MTASetEntitySelector),
                PropagationBasedCallGraphAnalysisScheduler,
                new TypePropagationAnalysisScheduler(MTASetEntitySelector)
            )
        )
        as.propertyStore.shutdown()
        as.propertyStore.getOrCreateInformation(
            TypePropagationVariant.id, TypePropagationVariant.MTA)

        validateProperties(
            as,
            classFilesWithAnnotations(as.project) ++
                declaredMethodsWithAnnotations(as.project) ++
                fieldsWithAnnotations(as.project),
            Set("AvailableTypes")
        )
    }

    describe("the FTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                new SimpleInstantiatedTypesAnalysisScheduler(FTASetEntitySelector),
                PropagationBasedCallGraphAnalysisScheduler,
                new TypePropagationAnalysisScheduler(FTASetEntitySelector)
            )
        )
        as.propertyStore.shutdown()
        as.propertyStore.getOrCreateInformation(
            TypePropagationVariant.id, TypePropagationVariant.FTA)

        validateProperties(
            as,
            classFilesWithAnnotations(as.project) ++
                declaredMethodsWithAnnotations(as.project) ++
                fieldsWithAnnotations(as.project),
            Set("AvailableTypes")
        )
    }

    describe("the CTA call graph analysis is executed") {
        val as = executeAnalyses(
            Set(
                new SimpleInstantiatedTypesAnalysisScheduler(CTASetEntitySelector),
                PropagationBasedCallGraphAnalysisScheduler,
                new TypePropagationAnalysisScheduler(CTASetEntitySelector)
            )
        )
        as.propertyStore.shutdown()
        as.propertyStore.getOrCreateInformation(
            TypePropagationVariant.id, TypePropagationVariant.CTA)

        validateProperties(
            as,
            classFilesWithAnnotations(as.project) ++
                declaredMethodsWithAnnotations(as.project) ++
                fieldsWithAnnotations(as.project),
            Set("AvailableTypes")
        )
    }
}