/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL
import java.util

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueFactory

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Annotation
import org.opalj.br.Annotations
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.ElementValuePairs
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.StringValue
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.properties.string.Constant
import org.opalj.fpcf.properties.string.Constants
import org.opalj.fpcf.properties.string.DomainLevel
import org.opalj.fpcf.properties.string.Dynamic
import org.opalj.fpcf.properties.string.Dynamics
import org.opalj.fpcf.properties.string.Failure
import org.opalj.fpcf.properties.string.Failures
import org.opalj.fpcf.properties.string.Invalid
import org.opalj.fpcf.properties.string.Invalids
import org.opalj.fpcf.properties.string.Level
import org.opalj.fpcf.properties.string.PartiallyConstant
import org.opalj.fpcf.properties.string.PartiallyConstants
import org.opalj.fpcf.properties.string.SoundnessMode
import org.opalj.log.OPALLogger
import org.opalj.tac.EagerDetachedTACAIKey
import org.opalj.tac.PV
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.cg.CallGraphKey
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.string.StringAnalysis
import org.opalj.tac.fpcf.analyses.string.UniversalStringConfig
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.analyses.string.flowanalysis.MethodStringFlowAnalysis
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l2.LazyL2StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l3.LazyL3StringAnalysis
import org.opalj.tac.fpcf.analyses.systemproperties.TriggeredSystemPropertiesAnalysisScheduler

// IMPROVE the test runner structure is far from optimal and could be reduced down to a simple test matrix. This however
// would require generating the wrapping "describes" and tag the tests using e.g. "asTagged" to be able to filter them
// during runs.
sealed abstract class StringAnalysisTest extends PropertiesTest {

    // The name of the method from which to extract PUVars to analyze.
    val nameTestMethod: String = "analyzeString"

    final val callGraphKey: CallGraphKey = RTACallGraphKey

    def level: Level
    def analyses: Iterable[ComputationSpecification[FPCFAnalysis]]
    def domainLevel: DomainLevel
    def soundnessMode: SoundnessMode

    override def createConfig(): Config = {
        val highSoundness = soundnessMode match {
            case SoundnessMode.HIGH => true
            case SoundnessMode.LOW  => false
        }

        super.createConfig()
            .withValue(UniversalStringConfig.HighSoundnessConfigKey, ConfigValueFactory.fromAnyRef(highSoundness))
            .withValue(StringAnalysis.DepthThresholdConfigKey, ConfigValueFactory.fromAnyRef(10))
            .withValue(
                MethodStringFlowAnalysis.ExcludedPackagesConfigKey,
                ConfigValueFactory.fromIterable(new util.ArrayList[String]())
            )
    }

    override def fixtureProjectPackage: List[String] = List("org/opalj/fpcf/fixtures/string")

    override def init(p: Project[URL]): Unit = {
        val domain = domainLevel match {
            case DomainLevel.L1 => classOf[DefaultDomainWithCFGAndDefUse[_]]
            case DomainLevel.L2 => classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
            case _              => throw new IllegalArgumentException(s"Invalid domain level for test definition: $domainLevel")
        }
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }
        p.updateProjectInformationKeyInitializationData(EagerDetachedTACAIKey) {
            case None    => m => domain.getConstructors.head.newInstance(p, m).asInstanceOf[Domain with RecordDefUse]
            case Some(_) => m => domain.getConstructors.head.newInstance(p, m).asInstanceOf[Domain with RecordDefUse]
        }

        val typeIterator = callGraphKey.getTypeIterator(p)
        p.updateProjectInformationKeyInitializationData(ContextProviderKey) { _ => typeIterator }
    }

    describe(s"using level=$level, domainLevel=$domainLevel, soundness=$soundnessMode") {
        describe(s"the string analysis is started") {
            var entities = Iterable.empty[(VariableContext, Method)]
            val project = FixtureProject.recreate { piKeyUniqueId => piKeyUniqueId != PropertyStoreKey.uniqueId }
            init(project)

            val as = executeAnalysesForProject(
                project,
                callGraphKey.allCallGraphAnalyses(project) ++ analyses,
                currentPhaseAnalyses => {
                    if (currentPhaseAnalyses.exists(_.derives.exists(_.pk == StringConstancyProperty))) {
                        val ps = project.get(PropertyStoreKey)
                        entities = determineEntitiesToAnalyze(project)
                        entities.foreach(entity => ps.force(entity._1, StringConstancyProperty.key))
                    }
                }
            )

            as.propertyStore.waitOnPhaseCompletion()
            as.propertyStore.shutdown()

            validateProperties(as, determineEAS(entities, project), Set("StringConstancy"))
        }
    }

    def determineEntitiesToAnalyze(project: Project[URL]): Iterable[(VariableContext, Method)] = {
        val tacProvider = project.get(EagerDetachedTACAIKey)
        val declaredMethods = project.get(DeclaredMethodsKey)
        val contextProvider = project.get(ContextProviderKey)
        project.allMethods
            .filter(_.runtimeInvisibleAnnotations.nonEmpty)
            .foldLeft(Seq.empty[(VariableContext, Method)]) { (entities, m) =>
                entities ++ extractPUVars(tacProvider(m)).map(e =>
                    (VariableContext(e._1, e._2, contextProvider.newContext(declaredMethods(m))), m)
                )
            }
    }

    /**
     * Extracts [[org.opalj.br.PUVar]]s from a set of statements. The locations of the [[org.opalj.br.PUVar]]s are
     * identified by the argument to the very first call to [[nameTestMethod]].
     *
     * @return Returns the arguments of the [[nameTestMethod]] as a PUVars list in the order in which they occurred.
     */
    def extractPUVars(tac: TACode[TACMethodParameter, V]): List[(Int, PV)] = {
        tac.cfg.code.instructions.filter {
            case VirtualMethodCall(_, _, _, name, _, _, _) => name == nameTestMethod
            case _                                         => false
        }.map { call => (call.pc, call.asVirtualMethodCall.params.head.asVar.toPersistentForm(tac.stmts)) }.toList
    }

    def determineEAS(
        entities: Iterable[(VariableContext, Method)],
        project:  Project[URL]
    ): Iterable[(VariableContext, String => String, List[Annotation])] = {
        val m2e = entities.groupBy(_._2).iterator.map(e =>
            e._1 -> (e._1, e._2.map(k => k._1))
        ).toMap

        var detectedMissingAnnotations = false
        // As entity, we need not the method but a tuple (PUVar, Method), thus this transformation
        val eas = methodsWithAnnotations(project).filter(am => m2e.contains(am._1)).flatMap { am =>
            val annotationsByIndex = getCheckableAnnotationsByIndex(project, am._3)
            m2e(am._1)._2.zipWithIndex.map {
                case (vc, index) =>
                    // Ensure every test that is annotated for at least one configuration is annotated for all
                    // configurations so we do not miss tests when adding new levels.
                    if (annotationsByIndex(index).isEmpty) {
                        OPALLogger.error(
                            "string analysis test setup",
                            s"Could not find annotations to check for #$index of ${am._1.toJava.substring(24)}"
                        )(project.logContext)
                        detectedMissingAnnotations = true
                    }

                    Tuple3(
                        vc,
                        { s: String => s"${am._2(s)} (#$index)" },
                        annotationsByIndex(index).toList
                    )
            }
        }

        if (detectedMissingAnnotations) {
            throw new IllegalStateException("Detected missing tests for this configuration!")
        }

        eas
    }

    private def getCheckableAnnotationsByIndex(project: Project[URL], as: Annotations): Map[Int, Annotations] = {
        def mapFailure(failureEvp: ElementValuePairs): Annotation = {
            if (soundnessMode == SoundnessMode.HIGH)
                new Annotation(
                    ObjectType(classOf[Dynamic].getName.replace(".", "/")),
                    failureEvp.appended(ElementValuePair("value", StringValue(".*")))
                )
            else
                new Annotation(
                    ObjectType(classOf[Invalid].getName.replace(".", "/")),
                    failureEvp
                )
        }

        as.flatMap {
            case a @ Annotation(annotationType, evp)
                if annotationType.toJavaClass == classOf[Constant]
                    || annotationType.toJavaClass == classOf[PartiallyConstant]
                    || annotationType.toJavaClass == classOf[Dynamic]
                    || annotationType.toJavaClass == classOf[Invalid] =>
                Seq((evp.head.value.asIntValue.value, a))

            case Annotation(annotationType, evp) if annotationType.toJavaClass == classOf[Failure] =>
                Seq((evp.head.value.asIntValue.value, mapFailure(evp)))

            case Annotation(annotationType, evp)
                if annotationType.toJavaClass == classOf[Constants]
                    || annotationType.toJavaClass == classOf[PartiallyConstants]
                    || annotationType.toJavaClass == classOf[Dynamics]
                    || annotationType.toJavaClass == classOf[Invalids] =>
                evp.head.value.asArrayValue.values.toSeq.map { av =>
                    val annotation = av.asAnnotationValue.annotation
                    (annotation.elementValuePairs.head.value.asIntValue.value, annotation)
                }

            case Annotation(annotationType, evp) if annotationType.toJavaClass == classOf[Failures] =>
                evp.head.value.asArrayValue.values.toSeq.map { av =>
                    val annotation = av.asAnnotationValue.annotation
                    (annotation.elementValuePairs.head.value.asIntValue.value, mapFailure(annotation.elementValuePairs))
                }

            case _ =>
                Seq.empty
        }.groupBy(_._1).map { kv =>
            val annotations = kv._2.map(_._2)
                .filter(fulfillsDomainLevel(project, _, domainLevel))
                .filter(fulfillsSoundness(project, _, soundnessMode))

            val matchingCurrentLevel = annotations.filter(fulfillsLevel(project, _, level))
            if (matchingCurrentLevel.isEmpty) {
                (kv._1, annotations.filter(fulfillsLevel(project, _, Level.TRUTH)))
            } else {
                (kv._1, matchingCurrentLevel)
            }
        }
    }

    private def fulfillsLevel(p: Project[URL], a: Annotation, l: Level): Boolean = {
        getValue(p, a, "levels").asArrayValue.values.exists(v => Level.valueOf(v.asEnumValue.constName) == l)
    }

    private def fulfillsDomainLevel(p: Project[URL], a: Annotation, dl: DomainLevel): Boolean = {
        getValue(p, a, "domains").asArrayValue.values.exists(v => DomainLevel.valueOf(v.asEnumValue.constName) == dl)
    }

    private def fulfillsSoundness(p: Project[URL], a: Annotation, soundness: SoundnessMode): Boolean = {
        getValue(p, a, "soundness").asArrayValue.values.exists { v =>
            SoundnessMode.valueOf(v.asEnumValue.constName) == soundness
        }
    }

    private def getValue(p: Project[URL], a: Annotation, name: String): ElementValue = {
        a.elementValuePairs.collectFirst {
            case ElementValuePair(`name`, value) => value
        }.orElse {
            // get default value ...
            p.classFile(a.annotationType.asObjectType).get.findMethod(name).head.annotationDefault
        }.get
    }
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L0StringAnalysisTest extends StringAnalysisTest {

    override final def level = Level.L0

    override final def analyses: Iterable[ComputationSpecification[FPCFAnalysis]] =
        LazyL0StringAnalysis.allRequiredAnalyses
}

class L0StringAnalysisWithL1DefaultDomainTest extends L0StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class L0StringAnalysisWithL2DefaultDomainTest extends L0StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class HighSoundnessL0StringAnalysisWithL2DefaultDomainTest extends L0StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.HIGH
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L1StringAnalysisTest extends StringAnalysisTest {

    override final def level = Level.L1

    override final def analyses: Iterable[ComputationSpecification[FPCFAnalysis]] = {
        LazyL1StringAnalysis.allRequiredAnalyses :+
            TriggeredSystemPropertiesAnalysisScheduler
    }
}

class L1StringAnalysisWithL1DefaultDomainTest extends L1StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class L1StringAnalysisWithL2DefaultDomainTest extends L1StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class HighSoundnessL1StringAnalysisWithL2DefaultDomainTest extends L1StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.HIGH
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l2.LazyL2StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L2StringAnalysisTest extends StringAnalysisTest {

    override def level = Level.L2

    override final def analyses: Iterable[ComputationSpecification[FPCFAnalysis]] = {
        LazyL2StringAnalysis.allRequiredAnalyses :+
            TriggeredSystemPropertiesAnalysisScheduler
    }
}

class L2StringAnalysisWithL1DefaultDomainTest extends L2StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class L2StringAnalysisWithL2DefaultDomainTest extends L2StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class HighSoundnessL2StringAnalysisWithL2DefaultDomainTest extends L2StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.HIGH
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l3.LazyL3StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L3StringAnalysisTest extends StringAnalysisTest {

    override def level = Level.L3

    override final def analyses: Iterable[ComputationSpecification[FPCFAnalysis]] = {
        LazyL3StringAnalysis.allRequiredAnalyses :+
            EagerFieldAccessInformationAnalysis :+
            TriggeredSystemPropertiesAnalysisScheduler
    }

    override def init(p: Project[URL]): Unit = {
        super.init(p)

        p.updateProjectInformationKeyInitializationData(FieldAccessInformationKey) {
            case None               => Seq(EagerFieldAccessInformationAnalysis)
            case Some(requirements) => requirements :+ EagerFieldAccessInformationAnalysis
        }
    }
}

class L3StringAnalysisWithL1DefaultDomainTest extends L3StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class L3StringAnalysisWithL2DefaultDomainTest extends L3StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.LOW
}

class HighSoundnessL3StringAnalysisWithL2DefaultDomainTest extends L3StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2
    override def soundnessMode: SoundnessMode = SoundnessMode.HIGH
}
