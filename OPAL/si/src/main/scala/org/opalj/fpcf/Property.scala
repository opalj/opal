/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * An ''immutable information'' associated with an entity. Each property belongs to exactly one
 * property kind specified by the [[PropertyKey]]. For details regarding the semantics of
 * a property see [[EOptionP#ub]].
 *
 * ==Implementation Requirements==
 * Properties have to be (effectively) immutable when passed to the framework. If a property
 * is mutable and (by some analysis) mutated the overall results will most likely no longer
 * be deterministic!
 *
 * ===Structural Equality===
 * Each implementation of the property trait has to implement an `equals` method that
 * determines if two properties are equal.
 *
 * @author Michael Eichberg
 */
trait Property extends PropertyMetaInformation {

    /**
     * Equality of properties has to be based on structural equality!
     */
    override def equals(other: Any): Boolean

    //
    //
    // IMPLEMENTATION PRIVATE METHODS
    //
    //

    /**
     * Returns `true` if this property inherits from [[OrderedProperty]].
     */
    final def isOrderedProperty: Boolean = this.isInstanceOf[OrderedProperty]

    /**
     * Returns `this` if this property inherits from [[OrderedProperty]].
     *
     * Used, e.g., by the framework to support debugging analyses.
     */
    final def asOrderedProperty: OrderedProperty = this.asInstanceOf[OrderedProperty]

}
