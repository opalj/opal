/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.xl

import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.{FinalEP, Property, PropertyStore}
import org.opalj.tac.cg.TypeIteratorKey
import org.opalj.tac.fpcf.analyses.cg.TypeIterator
import org.opalj.tac.fpcf.analyses.pointsto.longToAllocationSite
import org.opalj.xl.Coordinator.ScriptEngineInstance
import org.opalj.xl.utility.AnalysisResult
import org.opalj.xl.utility.InterimAnalysisResult

class TAJSEnvironmentMatcher extends AbstractPropertyMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val singleAnnotation = ObjectType("org/opalj/fpcf/properties/xl/JSEnvironment")
        if (a.annotationType == singleAnnotation) {
            validateSingleAnnotation(p, as, entity, a, properties)
        } else {
            Some("Invalid annotation.")
        }
    }

    private def findElement(
        elements:       ElementValuePairs,
        annotationName: String
    ): Option[ElementValue] = {
        elements.find(_.name == annotationName).map(_.value)
    }
    private def validateSingleAnnotation(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType

        val bindings = getValue(p, annotationType, a.elementValuePairs, "bindings").asArrayValue.values.map(_.asAnnotationValue.annotation).map(
            a => (
                findElement(a.elementValuePairs, "identifier").get.asStringValue.value,
                findElement(a.elementValuePairs, "value").get.asStringValue.value,

            )
        ).toSet
        implicit val ps: PropertyStore = p.get(PropertyStoreKey)
        implicit val typeIterator: TypeIterator = p.get(TypeIteratorKey)
        val m = entity.asInstanceOf[Method]
        //val allProperties = ps.properties(m).toSet
        val allEntities = ps.entities(AnalysisResult.key).toSet
        val scriptEngineInstances = allEntities.flatMap {
            case FinalEP(scriptEngineInstance: ScriptEngineInstance[_], _) => Option(scriptEngineInstance)
            case _ => None
        }
        val scriptEnginesInMethod = scriptEngineInstances
            .filter(sei => longToAllocationSite(sei.element.asInstanceOf[Long])._1.method.definedMethod == m).toSet

        val tajsMapping = scriptEnginesInMethod.flatMap(se =>
            (ps(se, AnalysisResult.key) match {
                case FinalEP(_, InterimAnalysisResult(tajsStore)) => tajsStore
                case _                                            => throw new Exception(s"no TAJS result available for method ${m.fullyQualifiedSignature}");
            }))

        val testBindings = tajsMapping.toSet.map((kv: (Any, Any)) => (kv._1.toString, kv._2.toString))

        val missingBindings = bindings diff testBindings
        if (missingBindings.nonEmpty) {
            Some(s"missing expected TAJS bindings. expected: $bindings actual $testBindings")
        } else {
            None
        }
    }
}
