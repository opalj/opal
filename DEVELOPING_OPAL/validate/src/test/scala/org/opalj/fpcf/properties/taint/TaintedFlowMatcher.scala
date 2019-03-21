/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AnnotationLike, ObjectType}
import org.opalj.fpcf.{Entity, Property}
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.tac.fpcf.analyses.Taint

class TaintedFlowMatcher extends AbstractPropertyMatcher {

  def validateProperty(
      p: SomeProject,
      as: Set[ObjectType],
      entity: Entity,
      a: AnnotationLike,
      properties: Traversable[Property]
  ): Option[String] = {
    val flows = properties.head
      .asInstanceOf[Taint]
      .flows
      .values
      .fold(Set.empty)((acc, facts) ⇒ acc ++ facts)
    println(flows)
    None
  }
}
