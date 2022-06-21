/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package callgraph

import org.opalj.br.AnnotationLike
import org.opalj.br.ArrayValue
import org.opalj.br.ClassValue
import org.opalj.br.DefinedMethod
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.FieldType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.StringValue
import org.opalj.br.VoidType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callees

import scala.collection.immutable.ArraySeq

class DirectCallMatcher extends AbstractPropertyMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        // If the entity is annotated with a single annotation, we receive a DirectCall annotation.
        // If it is annotated with multiple DirectCall annotations, we receive a single DirectCalls
        // container annotation.
        val singleAnnotation = ObjectType("org/opalj/fpcf/properties/callgraph/DirectCall")
        val containerAnnotation = ObjectType("org/opalj/fpcf/properties/callgraph/DirectCalls")

        if (a.annotationType == singleAnnotation) {
            validateSingleAnnotation(p, as, entity, a, properties)

        } else if (a.annotationType == containerAnnotation) {
            // Get sub-annotations from the container annotation.
            val subAnnotations: ArraySeq[AnnotationLike] =
                getValue(p, containerAnnotation, a.elementValuePairs, "value")
                    .asArrayValue.values.map(a => a.asAnnotationValue.annotation)

            // Validate each sub-annotation individually.
            val validationResults =
                subAnnotations.map(validateSingleAnnotation(p, as, entity, _, properties))
            val errors = validationResults.filter(_.isDefined)

            if (errors.nonEmpty) {
                Some(errors.mkString(", "))
            } else {
                None
            }

        } else {
            Some("Invalid annotation.")
        }
    }

    private def validateSingleAnnotation(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType

        // Get call graph analyses for which this annotation applies.
        val analysesElementValues: Seq[ElementValue] =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        // If none of the annotated analyses match the executed ones, return...
        // If the list of specified analyses is empty, we assume the annotation applies to all
        // call graph algorithms, so we don't exit early.
        if (analyses.nonEmpty && !analyses.exists(as.contains))
            return None;

        implicit val ps: PropertyStore = p.get(PropertyStoreKey)
        implicit val typeProvider: TypeProvider = p.get(TypeProviderKey)

        val calleesP = {
            properties.find(_.isInstanceOf[Callees]) match {
                case Some(property) => property.asInstanceOf[Callees]
                case None           => return Some("Callees property is missing.");
            }
        }

        val callsiteCode = entity.asInstanceOf[DefinedMethod].definedMethod.body match {
            case Some(code) => code
            case None       => return Some("Code of call site is not available.");
        }

        // Retrieve all calls resolved by the call graph algorithm.
        val callees = for {
            callerContext <- calleesP.callerContexts
            pc <- calleesP.callSitePCs(callerContext)
            calleeContext <- calleesP.callees(callerContext, pc)
            lineNr = callsiteCode.lineNumber(pc)
        } yield {
            val callee = calleeContext.method
            (
                lineNr.getOrElse(-1),
                callee.declaringClassType.toJVMTypeName,
                callee.name,
                callee.descriptor
            )
        }

        val resolvedCalleesSet = callees.toSet

        // Fetch values from the annotation.
        // TODO clean these up and move them into some helper?
        // note: there is a helper class in JCG that does something similar
        val lineNumber = getValue(p, annotationType, a.elementValuePairs, "line").asIntValue.value
        val methodName = getValue(p, annotationType, a.elementValuePairs, "name").asStringValue.value
        val resolvedTargets = {
            val av = a.elementValuePairs collectFirst {
                case ElementValuePair("resolvedTargets", ArrayValue(ab)) =>
                    ab.toIndexedSeq.map(_.asInstanceOf[StringValue].value)
            }
            av.getOrElse(List())
        }
        val returnType = a.elementValuePairs.find(_.name == "returnType") match {
            case Some(ElementValuePair(_, ClassValue(declType))) => declType
            case None                                            => VoidType
            case _                                               => sys.error("Invalid annotation.")
        }
        val parameterTypes = a.elementValuePairs.find(_.name == "parameterType") match {
            case Some(ElementValuePair(_, ArrayValue(params))) =>
                params.toArray.map(ev => ev.asInstanceOf[ClassValue].value.asFieldType)
            case None => Array[FieldType]()
            case _    => sys.error("Invalid annotation.")
        }

        val parametersArray = ArraySeq.unsafeWrapArray(parameterTypes)
        val descriptor = MethodDescriptor(parametersArray, returnType)

        val minimumExpectedCalleesSet = resolvedTargets.map {
            (lineNumber, _, methodName, descriptor)
        }.toSet

        // Calculate which expected callees are missing from the computed ones.
        val missingCalleesSet = minimumExpectedCalleesSet diff resolvedCalleesSet

        // TODO prohibited targets (is in the annotation, not sure we need this though)

        if (missingCalleesSet.nonEmpty) {
            // TODO more detailed reporting?
            Some(s"${missingCalleesSet.size} unresolved targets for call to ${methodName} in line ${lineNumber}")
        } else {
            None
        }
    }
}
