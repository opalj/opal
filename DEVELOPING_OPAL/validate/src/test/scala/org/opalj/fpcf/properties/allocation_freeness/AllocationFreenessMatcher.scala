/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package allocation_freeness

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.AllocationFreeness
import org.opalj.br.fpcf.properties

/**
 * Base trait for matchers that match a method's `AllocationFreeness` property.
 *
 * @author Dominik Helm
 */
sealed abstract class AllocationFreenessMatcher(
        val property: AllocationFreeness
)
    extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists(_ match {
            case `property` => true
            case _          => false
        })) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

/**
 * Matches a method's `AllocationFreeness` property. The match is successful if the method has the
 * property [[org.opalj.br.fpcf.properties.AllocationFreeMethod]].
 */
class AllocationFreeMethodMatcher
    extends AllocationFreenessMatcher(properties.AllocationFreeMethod)

/**
 * Matches a method's `AllocationFreeness` property. The match is successful if the method has the
 * property [[org.opalj.br.fpcf.properties.MethodWithAllocations]].
 */
class MethodWithAllocationsMatcher
    extends AllocationFreenessMatcher(properties.MethodWithAllocations)
