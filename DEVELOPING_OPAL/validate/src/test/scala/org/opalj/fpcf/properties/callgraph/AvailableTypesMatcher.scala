/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package callgraph

import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.tac.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import scala.collection.mutable

/**
 * Matches AvailableTypes annotations to the values computed cooperatively by a
 * dataflow-based call graph analysis.
 *
 * @author Andreas Bauer
 */
class AvailableTypesMatcher extends AbstractPropertyMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {

        val annotationType = a.annotationType.asObjectType

        // Get the set of type propagation variants for which this annotation applies.
        val variantsElementValues: Seq[ElementValue] =
            getValue(p, annotationType, a.elementValuePairs, "variants").asArrayValue.values
        val variants = variantsElementValues.map(ev => TypePropagationVariant.valueOf(ev.asEnumValue.constName))

        // Get the variant which was actually executed.
        val executedVariant =
            p.get(PropertyStoreKey).getInformation[TypePropagationVariant](TypePropagationVariant.tag) match {
                case Some(variant) => variant
                case None          => sys.error("type propagation variant must be registered")
            }

        // If none of the annotated variants match the executed ones, return...
        // If the list of specified variants is empty, we assume the annotation applies to all
        // of them, so we don't exit early.
        if (variants.nonEmpty && !variants.contains(executedVariant))
            return None;

        val instantiatedTypes = {
            properties.find(_.isInstanceOf[InstantiatedTypes]) match {
                case Some(prop) =>
                    prop.asInstanceOf[InstantiatedTypes].types
                case None =>
                    implicit val ctx: LogContext = p.logContext
                    OPALLogger.warn("property matcher", s"Expected property InstantiatedTypes was not computed for $entity.")
                    UIDSet.empty[ReferenceType]
            }
        }.toSet

        val expectedTypeNames: Seq[String] =
            getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "value").asArrayValue.values
                .map(ev => ev.asStringValue.value)

        val expectedTypes = expectedTypeNames.map(ReferenceType(_)).toSet

        val missingTypes = expectedTypes diff instantiatedTypes
        val additionalTypes = instantiatedTypes diff expectedTypes

        val isInvalid = missingTypes.nonEmpty || additionalTypes.nonEmpty
        if (isInvalid) {
            val errorMsg = new mutable.StringBuilder()
            errorMsg.append(s"Entity: $entity.\n")
            if (missingTypes.nonEmpty) {
                errorMsg.append(s"Expected types that were missing: \n* ${missingTypes.mkString("\n* ")}\n")
            }
            if (additionalTypes.nonEmpty) {
                errorMsg.append(s"Computed types which were unexpected: \n* ${additionalTypes.mkString("\n* ")}\n")
            }
            Some(errorMsg.toString())
        } else {
            None
        }
    }
}
