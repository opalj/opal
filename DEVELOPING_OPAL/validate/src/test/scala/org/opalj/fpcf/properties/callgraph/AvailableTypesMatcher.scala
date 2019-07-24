/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package callgraph

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.UIDSet
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.ElementValue
import org.opalj.br.ReferenceType

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
        properties: Traversable[Property]
    ): Option[String] = {

        if (as contains ObjectType("org/opalj/tac/fpcf/analyses/cg/xta/XTATypePropagationAnalysis")) {
            ()
        }

        val annotationType = a.annotationType.asObjectType

        // Get call graph analyses for which this annotation applies.
        val analysesElementValues: Seq[ElementValue] =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)

        // If none of the annotated analyses match the executed ones, return...
        // If the list of specified analyses is empty, we assume the annotation applies to all
        // call graph algorithms, so we don't exit early.
        if (analyses.nonEmpty && !analyses.exists(as.contains))
            return None;

        val instantiatedTypes = {
            properties.find(_.isInstanceOf[InstantiatedTypes]) match {
                case Some(prop) ⇒
                    prop.asInstanceOf[InstantiatedTypes].types
                case None ⇒
                    implicit val ctx: LogContext = p.logContext
                    // TODO AB maybe this should be an error after all, re-check later
                    OPALLogger.warn("property matcher", s"Expected property InstantiatedTypes was not computed for $entity.")
                    UIDSet.empty[ReferenceType]
            }
        }.toSet

        val expectedTypeNames: Seq[String] =
            getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "value").asArrayValue.values
                .map(ev ⇒ ev.asStringValue.value)

        val expectedTypes = expectedTypeNames.map(ReferenceType(_)).toSet

        val missingTypes = expectedTypes diff instantiatedTypes
        val additionalTypes = instantiatedTypes diff expectedTypes

        val isInvalid = missingTypes.nonEmpty || additionalTypes.nonEmpty
        if (isInvalid) {
            val errorMsg = StringBuilder.newBuilder
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
