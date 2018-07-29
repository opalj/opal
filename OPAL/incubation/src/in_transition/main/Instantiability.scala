/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

sealed trait InstantiabilityPropertyMetaInformation extends PropertyMetaInformation {
    type Self = Instantiability
}

/**
 * This is a common trait for all Instantiability properties which can be emitted to the
 * PropertyStore. It describes the instantibility of a class entity.
 */
sealed trait Instantiability extends Property with InstantiabilityPropertyMetaInformation {
    final def key = Instantiability.key // All instances have to share the SAME key!
}

/**
 * A companion object for the Instantiability trait. It holds the key, which is shared by
 * all properties derived from the Instantiability property, as well as it defines defines
 * the (sound) fall back if the property is not computed but requested by another analysis.
 */
object Instantiability extends InstantiabilityPropertyMetaInformation {
    final val key = PropertyKey.create[Instantiability]("Instantiability", Instantiable)
}

/**
 * NotInstantiable should be used for not instantiable classes.
 *
 * @example
 * {{{
 * public class Foo {
 *      private Foo(){}
 * }
 * }}}
 *
 * Foo is not instantiable because it can not be instantiated except
 * by Foo itself which does never call the private constructor.
 */
case object NotInstantiable extends Instantiability { final val isRefinable: Boolean = false }

/**
 * Should be assigned to classes which can be instantiated or are instantiated.
 */
case object Instantiable extends Instantiability { final val isRefinable: Boolean = false }

/**
 * Classes that are MaybeInstantiable lack of information. E.g., we don't know whether it is
 * instantiable or not.
 */
case object MaybeInstantiable extends Instantiability { final val isRefinable: Boolean = true }
