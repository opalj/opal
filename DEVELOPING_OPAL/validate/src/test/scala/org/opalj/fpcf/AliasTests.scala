/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.ai.domain.l1
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.AnnotationLike
import org.opalj.br.analyses.Project
import org.opalj.br.ClassValue
import org.opalj.br.StringValue
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.cg.TypeBasedPointsToCallGraphKey
import org.opalj.tac.fpcf.analyses.alias.AliasDS
import org.opalj.tac.fpcf.analyses.alias.AliasEntity
import org.opalj.tac.fpcf.analyses.alias.AliasFP
import org.opalj.tac.fpcf.analyses.alias.AliasNull
import org.opalj.tac.fpcf.analyses.alias.AliasReturnValue
import org.opalj.tac.fpcf.analyses.alias.AliasSourceElement
import org.scalatest.Ignore

import java.net.URL
import scala.collection.mutable.ArrayBuffer

/**
 * Tests if the alias properties defined in the classes of the package org.opalj.fpcf.fixtures.alias (and it's subpackage)
 * are computed correctly.
 */
@Ignore
class AliasTests extends PropertiesTest {

    override def fixtureProjectPackage: List[String] = {
        List("org/opalj/fpcf/fixtures/alias")
    }

    override def init(p: Project[URL]): Unit = {

        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) { _ =>

            Set[Class[_ <: AnyRef]](classOf[l1.DefaultDomainWithCFGAndDefUse[URL]])
        }

        p.get(TypeBasedPointsToCallGraphKey)

    }

    describe("run all alias analyses") {

        val as = executeAnalyses(
            Set( //TODO add analyses to execute
            )
        )

        as.propertyStore.shutdown()

        val allocations = allocationSitesWithAnnotations(as.project).flatMap { case (ds, fun, a) => getAliasAnnotations(a.head).map((ds, fun, _)) }
        val formalParameters = explicitFormalParametersWithAnnotations(as.project).flatMap { case (ds, fun, a) => getAliasAnnotations(a.head).map((ds, fun, _)) }
        val methods = methodsWithAnnotations(as.project).flatMap { case (m, fun, a) => getAliasAnnotations(a.head).map((m, fun, _)) }

        val simpleContexts = as.project.get(SimpleContextsKey)
        val declaredMethods = as.project.get(DeclaredMethodsKey)

        // The annotations only contain one of the two sourceElements of an alias property.
        // Therefore, we first have to combine elements with the same id and store them in this ArrayBuffer.
        val properties: ArrayBuffer[(AliasEntity, String => String, Iterable[AnnotationLike])] = ArrayBuffer.empty

        val IDToDs: Iterable[(String, (AliasDS, String => String))] = allocations.map { case (ds, str, a) => getID(a) -> (AliasDS(ds, as.project), str) }
        val IDToFP: Iterable[(String, (AliasFP, String => String))] = formalParameters.map { case (fp, str, a) => getID(a) -> (AliasFP(fp), str) }
        val IDToM: Iterable[(String, (AliasReturnValue, String => String))] = methods.map { case (m, str, a) => getID(a) -> (AliasReturnValue(m, as.project), str) }

        val IDToEntity: Map[String, Iterable[(AliasSourceElement, String => String)]] = (IDToDs ++ IDToFP ++ IDToM).groupMap(_._1)(_._2)

        for ((e: Entity, str: (String => String), an: AnnotationLike) <- allocations ++ formalParameters ++ methods) {
            val element1: AliasSourceElement = AliasSourceElement(e)(as.project)
            val element: (AliasSourceElement, String => String) = if (isNullAlias(an)) {
                (new AliasNull, s => "null")
            } else {
                val matchingEntities = IDToEntity(getID(an)).filter(_._1 != element1)
                if (matchingEntities.isEmpty) {
                    throw new IllegalArgumentException("No other entity with id " + getID(an) + " found")
                }
                if (matchingEntities.size > 1) {
                    throw new IllegalArgumentException("Multiple other entities with id " + getID(an) + " found")
                }
                matchingEntities.head
            }

            val context = simpleContexts(declaredMethods(element1.method))
            val entity = AliasEntity(context, element1, element._1)

            // Don't add the same property twice
            if (!properties.exists(_._1 == entity)) {
                properties.addOne((entity, s => str(s) + element._2(s), Seq(an)))
            }
        }

        validateProperties(as, properties, Set("AliasProperty"))

        println("reachable methods: " + as.project.get(TypeBasedPointsToCallGraphKey).reachableMethods().toList.size)
    }

    /**
     * Returns the id of the alias relation that is described by the given annotation.
     * @param a The annotation that describes the alias relation.
     * @return The id of the alias relation.
     */
    private[this] def getID(a: AnnotationLike): String = {
        getStringValue(a, "testClass") + "." + getStringValue(a, "id")
    }

    /**
     * Returns the value of the given annotation element.
     * @param a The annotation.
     * @param element The name of the element.
     * @return The value of the element.
     */
    private[this] def getStringValue(a: AnnotationLike, element: String): String = {
        a.elementValuePairs.filter(_.name == element).head.value match {
            case str: StringValue => str.value
            case ClassValue(t)    => t.asObjectType.fqn
            case _                => throw new RuntimeException("Unexpected value type")
        }
    }

    /**
     * Returns true if the given annotation describes an alias relation with null.
     * @param a The annotation.
     * @return True if the given annotation describes an alias relation with null.
     */
    private[this] def isNullAlias(a: AnnotationLike): Boolean = {
        a.elementValuePairs.find(_.name == "aliasWithNull").exists(_.value.asBooleanValue.value)
    }

    /**
     * Returns all alias annotations that are contained in the given annotation.
     * The given annotation must be an alias annotation.
     * @param a The annotation.
     * @return All alias annotations that are contained in the given annotation.
     */
    private[this] def getAliasAnnotations(a: AnnotationLike): Iterable[AnnotationLike] = {
        getAliasAnnotations(a, "noAlias") ++ getAliasAnnotations(a, "mayAlias") ++ getAliasAnnotations(a, "mustAlias")
    }

    /**
     * Returns all alias annotations of the given type that are contained in the given annotation.
     * The given annotation must be an alias annotation.
     * @param a The annotation.
     * @param aliasType The type of the alias annotations.
     * @return All alias annotations of the given type that are contained in the given annotation.
     */
    private[this] def getAliasAnnotations(a: AnnotationLike, aliasType: String): Iterable[AnnotationLike] = {
        a.elementValuePairs.filter(_.name == aliasType).collect { case ev => ev.value.asArrayValue.values.map(_.asAnnotationValue.annotation) }.flatten
    }

}
