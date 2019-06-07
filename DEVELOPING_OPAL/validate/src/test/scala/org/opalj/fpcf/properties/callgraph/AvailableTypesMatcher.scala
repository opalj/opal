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
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes

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

        val instantiatedTypes = {
            properties.find(_.isInstanceOf[InstantiatedTypes]) match {
                case Some(prop) ⇒
                    prop.asInstanceOf[InstantiatedTypes].types
                case None       ⇒
                    implicit val ctx: LogContext = p.logContext
                    // TODO AB maybe this should be an error after all, re-check later
                    OPALLogger.warn("property matcher", s"Expected property InstantiatedTypes not computed for $entity.")
                    UIDSet.empty[ObjectType]
            }
        }.toSet

        val expectedTypeNames: Seq[String] =
            getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "value").asArrayValue.values
                .map(ev ⇒ ev.asStringValue.value)

        // TODO AB substring due to types being defined like "L...;", maybe should use different annotation format
        val expectedTypes = expectedTypeNames.map(tn ⇒ ObjectType(tn.substring(1, tn.length - 1))).toSet

        val missingTypes = expectedTypes diff instantiatedTypes
        val additionalTypes = instantiatedTypes diff expectedTypes

        val isInvalid = missingTypes.nonEmpty || additionalTypes.nonEmpty
        if (isInvalid) {
            val errorMsg = StringBuilder.newBuilder
            errorMsg.append(s"Entity: $entity.")
            if (missingTypes.nonEmpty) {
                errorMsg.append(s" Expected types that were missing: ${missingTypes.mkString(", ")}.")
            }
            if (additionalTypes.nonEmpty) {
                errorMsg.append(s" Computed types which were unexpected: ${additionalTypes.mkString(", ")}")
            }
            Some(errorMsg.toString())
        } else {
            None
        }
    }
}
