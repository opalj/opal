/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * A property which has an explicit name. This is particularly useful when we want to refer to a
 * property in the context of some test cases. In general, it should be tried that the name is
 * reasonably unique w.r.t. its usage scenario.
 */
trait ExplicitlyNamedProperty extends Property {

    /**
     * The name of the property.
     */
    def propertyName: String

}

/**
 * Defines an extractor for an [[ExplicitlyNamedProperty]].
 */
object ExplicitlyNamedProperty {

    def unapply(p: ExplicitlyNamedProperty): Some[String] = Some(p.propertyName)

}
