/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;

/**
 * Generic class with only a transitively immutable field.
 */
@TransitivelyImmutableClass("class has only a transitively immutable field")
class GenericCounterExampleTransitivelyImmutable<T> {

    @TransitivelyImmutableField("field n is non assignable and has a primitive type")
    @NonAssignableField("field is final")
    private final int n = 5;
}
