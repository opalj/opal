/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import java.net.URL

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.Annotation
import org.opalj.br.Annotations
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.tac.EagerDetachedTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.fieldaccess.EagerFieldAccessInformationAnalysis
import org.opalj.tac.fpcf.analyses.string.SEntity
import org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis
import org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis

sealed abstract class StringAnalysisTest extends PropertiesTest {

    // The name of the method from which to extract PUVars to analyze.
    val nameTestMethod: String = "analyzeString"

    def level: Int

    override def fixtureProjectPackage: List[String] = {
        StringAnalysisTest.getFixtureProjectPackages(level).toList
    }

    protected def allowedFQTestMethodsClassNames: Iterable[String] = {
        StringAnalysisTest.getAllowedFQTestMethodClassNamesUntilLevel(level)
    }

    /**
     * Resolves all test methods for this [[level]] and below while taking overrides into account. For all test methods,
     * [[extractPUVars]] is called with their [[TACode]].
     */
    def determineEntitiesToAnalyze(project: Project[URL]): Iterable[(Int, SEntity, Method)] = {
        val tacProvider = project.get(EagerDetachedTACAIKey)
        project.classHierarchy.allSuperclassesIterator(
            ObjectType(StringAnalysisTest.getAllowedFQTestMethodObjectTypeNameForLevel(level)),
            reflexive = true
        )(project).toList
            .filter(_.thisType.packageName.startsWith("org/opalj/fpcf/fixtures/string_analysis/"))
            .sortBy { cf => cf.thisType.simpleName.substring(1, 2).toInt }
            .foldRight(Seq.empty[Method]) { (cf, methods) =>
                methods ++ cf.methods.filterNot(m => methods.exists(_.name == m.name))
            }
            .filter {
                _.runtimeInvisibleAnnotations.foldLeft(false)((exists, a) =>
                    exists || StringAnalysisTest.isStringUsageAnnotation(a)
                )
            }
            .foldLeft(Seq.empty[(Int, SEntity, Method)]) { (entities, m) =>
                entities ++ extractPUVars(tacProvider(m)).map(e => (e._1, e._2, m))
            }
    }

    /**
     * Extracts [[org.opalj.tac.PUVar]]s from a set of statements. The locations of the [[org.opalj.tac.PUVar]]s are
     * identified by the argument to the very first call to [[nameTestMethod]].
     *
     * @return Returns the arguments of the [[nameTestMethod]] as a PUVars list in the order in which they occurred.
     */
    def extractPUVars(tac: TACode[TACMethodParameter, V]): List[(Int, SEntity)] = {
        tac.cfg.code.instructions.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) =>
                allowedFQTestMethodsClassNames.exists(_ == declClass.toJavaClass.getName) && name == nameTestMethod
            case _ => false
        }.map { call => (call.pc, call.asVirtualMethodCall.params.head.asVar.toPersistentForm(tac.stmts)) }.toList
    }

    def determineEAS(
        entities: Iterable[(Int, SEntity, Method)],
        project:  Project[URL]
    ): Iterable[((Int, SEntity, Method), String => String, List[Annotation])] = {
        val m2e = entities.groupBy(_._3).iterator.map(e => e._1 -> e._2.map(k => (k._1, k._2))).toMap
        // As entity, we need not the method but a tuple (PUVar, Method), thus this transformation
        methodsWithAnnotations(project).filter(am => m2e.contains(am._1)).flatMap { am =>
            m2e(am._1).zipWithIndex.map {
                case ((pc, puVar), index) =>
                    Tuple3(
                        (pc, puVar, am._1),
                        { s: String => s"${am._2(s)} (#$index)" },
                        List(StringAnalysisTest.getStringDefinitionsFromCollection(am._3, index))
                    )
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
        a.annotationType.toJavaClass.getName == "org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection"

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
    def getStringDefinitionsFromCollection(a: Annotations, index: Int): Annotation =
        a.head.elementValuePairs(1).value.asArrayValue.values(index).asAnnotationValue.annotation
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l0.LazyL0StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
class L0StringAnalysisTest extends StringAnalysisTest {

    override def level = 0

    override def init(p: Project[URL]): Unit = {
        val domain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }

        p.get(RTACallGraphKey)
    }

    describe("the org.opalj.fpcf.L0StringAnalysis is started") {
        val as = executeAnalyses(LazyL0StringAnalysis.allRequiredAnalyses)

        val entities = determineEntitiesToAnalyze(as.project)
        val newEntities = entities

        // it("can be executed without exceptions") {
        newEntities.foreach(as.propertyStore.force(_, StringConstancyProperty.key))

        as.propertyStore.waitOnPhaseCompletion()
        as.propertyStore.shutdown()

        validateProperties(as, determineEAS(newEntities, as.project), Set("StringConstancy"))
        // }
    }
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string.l1.LazyL1StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
class L1StringAnalysisTest extends StringAnalysisTest {

    override def level = 1

    override def init(p: Project[URL]): Unit = {
        val domain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(domain)
            case Some(requirements) => requirements + domain
        }
        p.updateProjectInformationKeyInitializationData(FieldAccessInformationKey) {
            case None               => Seq(EagerFieldAccessInformationAnalysis)
            case Some(requirements) => requirements :+ EagerFieldAccessInformationAnalysis
        }

        p.get(RTACallGraphKey)
    }

    describe("the org.opalj.fpcf.L1StringAnalysis is started") {
        val as = executeAnalyses(LazyL1StringAnalysis.allRequiredAnalyses)

        val entities = determineEntitiesToAnalyze(as.project)
            // Currently broken L1 Tests
            .filterNot(entity => entity._3.name.startsWith("cyclicDependencyTest"))
            .filterNot(entity => entity._3.name.startsWith("unknownHierarchyInstanceTest"))
            .filterNot(entity => entity._3.name.startsWith("crissCrossExample"))
        entities.foreach(as.propertyStore.force(_, StringConstancyProperty.key))

        as.propertyStore.waitOnPhaseCompletion()
        as.propertyStore.shutdown()

        validateProperties(as, determineEAS(entities, as.project), Set("StringConstancy"))
    }
}
