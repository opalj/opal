/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.known_types.single;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

/**
 * This class represents the counter-example in which the type of the object assigned to a field is not known
 */
@NonTransitivelyImmutableType("class is non transitively immutable and final")
@NonTransitivelyImmutableClass("class has only one non-transitively immutable field")
final class ConcreteTypeUnknown {

    @NonTransitivelyImmutableField("field has a mutable type")
    @NonAssignableField("field is final")
    private final Object object;

    public ConcreteTypeUnknown(Object object) {
        this.object = object;
    }
}
