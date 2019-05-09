/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey

/**
 * Determines if an object potentially leaks its self reference (`this`) by passing
 * assigning it to fields or passing it to methods which assign it to fields.
 * Hence, it computes a special escape information.
 *
 * Here, the self-reference escapes the scope of a class if:
 *  - ... it is assigned to a (static) field,
 *  - ... it is passed to a method that assigns it to a field,
 *  - ... it is stored in an array,
 *  - ... it is returned (recall that constructors have a void return type),
 *  - ... if a superclass leaks the self reference.
 *
 * This property can be used as a foundation for an analysis that determines whether
 * all instances created for a specific class never escape their creating methods and,
 * hence, respective types cannot occur outside the respective contexts.
 */
sealed trait SelfReferenceLeakage extends Property {

    final type Self = SelfReferenceLeakage

    final def key = SelfReferenceLeakage.Key
}

/**
 * Models the top of the self-references leakage lattice.
 */
case object DoesNotLeakSelfReference extends SelfReferenceLeakage

/**
 * Models the bottom of the lattice.
 */
case object LeaksSelfReference extends SelfReferenceLeakage

object SelfReferenceLeakage {

    final val Key = PropertyKey.create[ObjectType, SelfReferenceLeakage](
        name = "org.opalj.SelfReferenceLeakage",
        fallbackProperty = LeaksSelfReference
    )

}
