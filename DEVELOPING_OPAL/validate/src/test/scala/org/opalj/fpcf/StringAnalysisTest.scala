/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey

import java.net.URL
import org.opalj.br.Annotation
import org.opalj.br.Annotations
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.tac.EagerDetachedTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.V
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.string_analysis.SEntity
import org.opalj.tac.fpcf.analyses.string_analysis.l0.LazyL0StringAnalysis
import org.opalj.tac.fpcf.analyses.string_analysis.l1.LazyL1StringAnalysis

sealed abstract class StringAnalysisTest extends PropertiesTest {

    // The fully-qualified name of the class that contains the test methods.
    protected def fqTestMethodsClass: String
    // The name of the method from which to extract PUVars to analyze.
    protected def nameTestMethod: String

    /**
     * Extracts a `StringDefinitions` annotation from a `StringDefinitionsCollection` annotation.
     * Make sure that you pass an instance of `StringDefinitionsCollection` and that the element at
     * the given index really exists. Otherwise an exception will be thrown.
     *
     * @param a The `StringDefinitionsCollection` to extract a `StringDefinitions` from.
     * @param index The index of the element from the `StringDefinitionsCollection` annotation to
     *              get.
     * @return Returns the desired `StringDefinitions` annotation.
     */
    def getStringDefinitionsFromCollection(a: Annotations, index: Int): Annotation =
        a.head.elementValuePairs(1).value.asArrayValue.values(index).asAnnotationValue.annotation

    def determineEntitiesToAnalyze(project: Project[URL]): Iterable[(SEntity, Method)] = {
        var entitiesToAnalyze = Seq[(SEntity, Method)]()
        val tacProvider = project.get(EagerDetachedTACAIKey)
        project.allMethodsWithBody.filter {
            _.runtimeInvisibleAnnotations.foldLeft(false)((exists, a) =>
                exists || StringAnalysisTest.isStringUsageAnnotation(a)
            )
        } foreach { m =>
            entitiesToAnalyze = entitiesToAnalyze ++ extractPUVars(tacProvider(m)).map((_, m))
        }
        entitiesToAnalyze
    }

    /**
     * Extracts [[org.opalj.tac.PUVar]]s from a set of statements. The locations of the PUVar are
     * identified by the argument to the very first call to [[fqTestMethodsClass]]#[[nameTestMethod]].
     *
     * @param tac The tac from which to extract the PUVar, usually derived from the
     *            method that contains the call(s) to [[fqTestMethodsClass]]#[[nameTestMethod]].
     * @return Returns the arguments of the [[fqTestMethodsClass]]#[[nameTestMethod]] as a PUVars list in the
     *         order in which they occurred in the given statements.
     */
    def extractPUVars(tac: TACode[TACMethodParameter, V]): List[SEntity] = {
        tac.cfg.code.instructions.filter {
            case VirtualMethodCall(_, declClass, _, name, _, _, _) =>
                declClass.toJavaClass.getName == fqTestMethodsClass && name == nameTestMethod
            case _ => false
        }.map(_.asVirtualMethodCall.params.head.asVar.toPersistentForm(tac.stmts)).toList
    }

    def determineEAS(
        entities: Iterable[(SEntity, Method)],
        project:  Project[URL]
    ): Iterable[((SEntity, Method), String => String, List[Annotation])] = {
        val m2e = entities.groupBy(_._2).iterator.map(e => e._1 -> e._2.map(k => k._1)).toMap
        // As entity, we need not the method but a tuple (PUVar, Method), thus this transformation
        methodsWithAnnotations(project).filter(am => m2e.contains(am._1)).flatMap { am =>
            m2e(am._1).zipWithIndex.map {
                case (puVar, index) =>
                    Tuple3(
                        (puVar, am._1),
                        { s: String => s"${am._2(s)} (#$index)" },
                        List(getStringDefinitionsFromCollection(am._3, index))
                    )
            }
        }
    }
}

object StringAnalysisTest {

    /**
     * Takes an annotation and checks if it is a
     * [[org.opalj.fpcf.properties.string_analysis.StringDefinitions]] annotation.
     *
     * @param a The annotation to check.
     * @return True if the `a` is of type StringDefinitions and false otherwise.
     */
    def isStringUsageAnnotation(a: Annotation): Boolean =
        a.annotationType.toJavaClass.getName == "org.opalj.fpcf.properties.string_analysis.StringDefinitionsCollection"
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string_analysis.l0.L0StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
class IntraproceduralStringAnalysisTest extends StringAnalysisTest {

    override protected val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_analysis.intraprocedural.IntraProceduralTestMethods"
    override protected val nameTestMethod = "analyzeString"

    override def fixtureProjectPackage: List[String] = List(s"org/opalj/fpcf/fixtures/string_analysis/intraprocedural")

    override def init(p: Project[URL]): Unit = {
        val domain = classOf[DefaultPerformInvocationsDomainWithCFGAndDefUse[_]]
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None => Set(domain)
            case Some(requirements) => requirements + domain
        }

        p.get(RTACallGraphKey)
    }

    describe("the org.opalj.fpcf.IntraproceduralStringAnalysis is started") {
        val as = executeAnalyses(LazyL0StringAnalysis)

        val entities = determineEntitiesToAnalyze(as.project)
        val newEntities = entities
            //.filter(entity => entity._2.name.startsWith("tryCatchFinally"))
            //.filter(entity => entity._2.name.startsWith("tryCatchFinallyWithThrowable"))
            .filterNot(entity => entity._2.name.startsWith("switchNested"))
            .filterNot(entity => entity._2.name.startsWith("tryCatchFinallyWithThrowable"))
            .filterNot(entity => entity._2.name.startsWith("twoDefinitionsOneUsage"))
            .filterNot(entity => entity._2.name.startsWith("simpleStringConcat"))
            .filterNot(entity => entity._2.name.startsWith("multipleDefSites"))
            .filterNot(entity => entity._2.name.startsWith("fromConstantAndFunctionCall"))

        //it("can be executed without exceptions") {
            newEntities.foreach(as.propertyStore.force(_, StringConstancyProperty.key))

            as.propertyStore.shutdown()

            validateProperties(as, determineEAS(newEntities, as.project), Set("StringConstancy"))
        //}
    }
}

/**
 * Tests whether the [[org.opalj.tac.fpcf.analyses.string_analysis.l1.L1StringAnalysis]] works correctly with
 * respect to some well-defined tests.
 *
 * @author Maximilian Rüsch
 */
class InterproceduralStringAnalysisTest extends StringAnalysisTest {

    override protected val fqTestMethodsClass = "org.opalj.fpcf.fixtures.string_analysis.interprocedural.InterproceduralTestMethods"
    override protected val nameTestMethod = "analyzeString"

    override def fixtureProjectPackage: List[String] = List(s"org/opalj/fpcf/fixtures/string_analysis/interprocedural")

    override def init(p: Project[URL]): Unit = {
        p.get(RTACallGraphKey)
    }

    describe("the org.opalj.fpcf.InterproceduralStringAnalysis is started") {
        val as = executeAnalyses(LazyL1StringAnalysis)

        val entities = determineEntitiesToAnalyze(as.project) //.filter(entity => entity._2.name == "valueOfTest2")
        entities.foreach(as.propertyStore.force(_, StringConstancyProperty.key))

        as.propertyStore.shutdown()
        validateProperties(as, determineEAS(entities, as.project), Set("StringConstancy"))
    }
}
