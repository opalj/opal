/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package callgraph

import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

class VMReachableMethodMatcher extends AbstractPropertyMatcher {

    override def validateProperty(
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
            return None

        val callersP = {
            properties.find(_.isInstanceOf[Callers]) match {
                case Some(property) => property.asInstanceOf[Callers]
                case None           => return Some("Callers property is missing.");
            }
        }
        if (!callersP.hasVMLevelCallers) {
            Some(s"Method not VM Reachable")
        } else {
            None
        }
    }
}
