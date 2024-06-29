/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Annotation
import org.opalj.br.Annotations
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.fpcf.properties.string_analysis.DomainLevel
import org.opalj.tac.EagerDetachedTACAIKey
import org.opalj.tac.PV
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.string.VariableContext
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis
import org.opalj.tac.fpcf.analyses.systemproperties.EagerSystemPropertiesAnalysisScheduler

sealed abstract class StringAnalysisTest extends PropertiesTest {

    // The name of the method from which to extract PUVars to analyze.
    val nameTestMethod: String = "analyzeString"

    def level: Int
    def domainLevel: DomainLevel

    override final def init(p: Project[URL]): Unit = {
        val domain = domainLevel match {
            case DomainLevel.L1 => classOf[DefaultDomainWithCFGAndDefUse[_]]
            case DomainLevel.L2 => classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        }
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        initBeforeCallGraph(p)

        p.get(RTACallGraphKey)
    }

    def initBeforeCallGraph(p: Project[URL]): Unit = {}

    override def fixtureProjectPackage: List[String] = {
        StringAnalysisTest.getFixtureProjectPackages(level).toList
    }

    protected def allowedFQTestMethodsClassNames: Iterable[String] = {
        StringAnalysisTest.getAllowedFQTestMethodClassNamesUntilLevel(level)
    }

    /**
     * Resolves all test methods for this [[level]] and below while taking overrides into account. For all test methods,
     * [[extractPUVars]] is called with their [[TACode]].
     *
     * @return An [[Iterable]] containing the [[VariableContext]] to be analyzed and the method that has the relevant
     *         annotations attached.
     */
    def determineEntitiesToAnalyze(project: Project[URL]): Iterable[(VariableContext, Method)] = {
        val tacProvider = project.get(EagerDetachedTACAIKey)
        val declaredMethods = project.get(DeclaredMethodsKey)
        val contextProvider = project.get(ContextProviderKey)
        project.classHierarchy.allSuperclassesIterator(
            ObjectType(StringAnalysisTest.getAllowedFQTestMethodObjectTypeNameForLevel(level)),
            reflexive = true
        )(project).toList
            .filter(_.thisType.packageName.startsWith("org/opalj/fpcf/fixtures/string_analysis/"))
            .sortBy { cf => cf.thisType.simpleName.substring(1, 2).toInt }
            .foldLeft(Map.empty[Method, Method]) { (implementationsToAnnotations, cf) =>
                implementationsToAnnotations ++ cf.methods.map { m =>
                    (
                        implementationsToAnnotations.find(kv =>
                            kv._1.name == m.name && kv._1.descriptor == m.descriptor
                        ).map(_._1).getOrElse(m),
                        m
                    )
                }
            }
            .filter {
                _._1.runtimeInvisibleAnnotations.foldLeft(false)((exists, a) =>
                    exists || StringAnalysisTest.isStringUsageAnnotation(a)
                )
            }
            .filter {
                _._1.runtimeInvisibleAnnotations.forall { a =>
                    val r = isAllowedDomainLevel(a)
                    r.isEmpty || r.get
                }
            }
            .foldLeft(Seq.empty[(VariableContext, Method)]) { (entities, m) =>
                entities ++ extractPUVars(tacProvider(m._1)).map(e =>
                    (VariableContext(e._1, e._2, contextProvider.newContext(declaredMethods(m._1))), m._2)
                )
            }
    }

    /**
     * Extracts [[org.opalj.tac.PUVar]]s from a set of statements. The locations of the [[org.opalj.tac.PUVar]]s are
     * identified by the argument to the very first call to [[nameTestMethod]].
     *
     * @return Returns the arguments of the [[nameTestMethod]] as a PUVars list in the order in which they occurred.
     */
    def extractPUVars(tac: TACode[TACMethodParameter, V]): List[(Int, PV)] = {
        tac.cfg.code.instructions.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) =>
                allowedFQTestMethodsClassNames.exists(_ == declClass.toJavaClass.getName) && name == nameTestMethod
            case _ => false
        }.map { call => (call.pc, call.asVirtualMethodCall.params.head.asVar.toPersistentForm(tac.stmts)) }.toList
    }

    def determineEAS(
        entities: Iterable[(VariableContext, Method)],
        project:  Project[URL]
    ): Iterable[(VariableContext, String => String, List[Annotation])] = {
        val m2e = entities.groupBy(_._2).iterator.map(e =>
            e._1 -> (e._1, e._2.map(k => k._1))
        ).toMap
        // As entity, we need not the method but a tuple (PUVar, Method), thus this transformation
        methodsWithAnnotations(project).filter(am => m2e.contains(am._1)).flatMap { am =>
            m2e(am._1)._2.zipWithIndex.map {
                case (vc, index) =>
                    Tuple3(
                        vc,
                        { s: String => s"${am._2(s)} (#$index)" },
                        List(StringAnalysisTest.getStringDefinitionsFromCollection(am._3, index))
                    )
            }
        }
    }

    def isAllowedDomainLevel(a: Annotation): Option[Boolean] = {
        if (a.annotationType.toJava != "org.opalj.fpcf.properties.string_analysis.AllowedDomainLevels") None
        else Some {
            a.elementValuePairs.head.value.asArrayValue.values.exists { v =>
                DomainLevel.valueOf(v.asEnumValue.constName) == domainLevel
            }
        }
    }
}

object StringAnalysisTest {

    def getFixtureProjectPackages(level: Int): Seq[String] = {
        Range.inclusive(0, level).map(l => s"org/opalj/fpcf/fixtures/string_analysis/l$l")
    }

    def getAllowedFQTestMethodClassNamesUntilLevel(level: Int): Seq[String] = {
        Range.inclusive(0, level).map(l => s"org.opalj.fpcf.fixtures.string_analysis.l$l.L${l}TestMethods")
    }

    def getAllowedFQTestMethodObjectTypeNameForLevel(level: Int): String = {
        s"org/opalj/fpcf/fixtures/string_analysis/l$level/L${level}TestMethods"
    }

    /**
     * Takes an annotation and checks if it is a
     * [[org.opalj.fpcf.properties.string_analysis.StringDefinitions]] annotation.
     *
     * @param a The annotation to check.
     * @return True if the `a` is of type StringDefinitions and false otherwise.
     */
    def isStringUsageAnnotation(a: Annotation): Boolean =
        a.annotationType.toJava == "org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection"

    /**
     * Extracts a `StringDefinitions` annotation from a `StringDefinitionsCollection` annotation.
     * Make sure that you pass an instance of `StringDefinitionsCollection` and that the element at
     * the given index really exists. Otherwise an exception will be thrown.
     *
     * @param a     The `StringDefinitionsCollection` to extract a `StringDefinitions` from.
     * @param index The index of the element from the `StringDefinitionsCollection` annotation to
     *              get.
     * @return Returns the desired `StringDefinitions` annotation.
     */
    def getStringDefinitionsFromCollection(a: Annotations, index: Int): Annotation = {
        val collectionOpt = a.find(isStringUsageAnnotation)
        if (collectionOpt.isEmpty) {
            throw new IllegalArgumentException(
                "Tried to collect string definitions from method that does not define them!"
            )
        }

        collectionOpt.get.elementValuePairs(1).value.asArrayValue.values(index).asAnnotationValue.annotation
    }
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L0StringAnalysisTest extends StringAnalysisTest {

    override final def level = 0

    final def runL0Tests(): Unit = {
        describe("the org.opalj.fpcf.L0StringAnalysis is started") {
            val as = executeAnalyses(
                LazyL0StringAnalysis.allRequiredAnalyses :+
                    EagerSystemPropertiesAnalysisScheduler
            )

            val entities = determineEntitiesToAnalyze(as.project)
            entities.foreach(entity => as.propertyStore.force(entity._1, StringConstancyProperty.key))

            as.propertyStore.waitOnPhaseCompletion()
            as.propertyStore.shutdown()

            validateProperties(as, determineEAS(entities, as.project), Set("StringConstancy"))
        }
    }
}

class L0StringAnalysisWithL1DefaultDomainTest extends L0StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1

    describe("using the l1 default domain") { runL0Tests() }
}

class L0StringAnalysisWithL2DefaultDomainTest extends L0StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2

    describe("using the l2 default domain") { runL0Tests() }
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class L1StringAnalysisTest extends StringAnalysisTest {

    override def level = 1

    override def initBeforeCallGraph(p: Project[URL]): Unit = {
        p.updateProjectInformationKeyInitializationData(FieldAccessInformationKey) {
            case None               => Seq(EagerFieldAccessInformationAnalysis)
            case Some(requirements) => requirements :+ EagerFieldAccessInformationAnalysis
        }
    }

    final def runL1Tests(): Unit = {
        describe("the org.opalj.fpcf.L1StringAnalysis is started") {
            val as = executeAnalyses(
                LazyL1StringAnalysis.allRequiredAnalyses :+
                    EagerFieldAccessInformationAnalysis :+
                    EagerSystemPropertiesAnalysisScheduler
            )

            val entities = determineEntitiesToAnalyze(as.project)
                // Currently broken L1 Tests
                .filterNot(entity => entity._2.name.startsWith("cyclicDependencyTest"))
            entities.foreach(entity => as.propertyStore.force(entity._1, StringConstancyProperty.key))

            as.propertyStore.waitOnPhaseCompletion()
            as.propertyStore.shutdown()

            validateProperties(as, determineEAS(entities, as.project), Set("StringConstancy"))
        }
    }
}

class L1StringAnalysisWithL1DefaultDomainTest extends L1StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L1

    describe("using the l1 default domain") { runL1Tests() }
}

class L1StringAnalysisWithL2DefaultDomainTest extends L1StringAnalysisTest {

    override def domainLevel: DomainLevel = DomainLevel.L2

    describe("using the l2 default domain") { runL1Tests() }
}
