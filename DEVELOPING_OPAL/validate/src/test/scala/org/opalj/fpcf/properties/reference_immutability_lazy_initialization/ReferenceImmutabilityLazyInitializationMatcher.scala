/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.reference_immutability_lazy_initialization

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.LazyInitialization
import org.opalj.br.fpcf.properties.NoLazyInitialization
import org.opalj.br.fpcf.properties.NotThreadSafeLazyInitialization
import org.opalj.br.fpcf.properties.ReferenceImmutabilityLazyInitialization
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher

/**
 * @author Tobias Peter Roth
 */
class ReferenceImmutabilityLazyInitializationMatcher(
    val property: ReferenceImmutabilityLazyInitialization
) extends AbstractPropertyMatcher {

  final private val PropertyReasonID = 0

  override def isRelevant(
      p: SomeProject,
      as: Set[ObjectType],
      entity: Object,
      a: AnnotationLike
  ): Boolean = {
    val annotationType = a.annotationType.asObjectType

    val analysesElementValues =
      getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
    val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

    analyses.exists(as.contains)
  }

  def validateProperty(
      p: SomeProject,
      as: Set[ObjectType],
      entity: Entity,
      a: AnnotationLike,
      properties: Traversable[Property]
  ): Option[String] = {
    if (!properties.exists(p => p == property)) {
      // ... when we reach this point the expected property was not found.
      Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
    } else {
      None
    }
  }
}

class NoLazyInitializationMatcher
    extends ReferenceImmutabilityLazyInitializationMatcher(NoLazyInitialization)

class NotThreadSafeInitializationMatcher
    extends ReferenceImmutabilityLazyInitializationMatcher(NotThreadSafeLazyInitialization)

class LazyInitializationMatcher
    extends ReferenceImmutabilityLazyInitializationMatcher(LazyInitialization)
