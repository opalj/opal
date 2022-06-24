/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.closed_world.known_types.single;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class represents the case in which a single known object is assigned to a field.
 */
@MutableType("Class is not final")
@TransitivelyImmutableClass("Class has at least transitive immutable field")
class ConcreteObjectInstanceAssigned {

    @TransitivelyImmutableField("Field has a transitively immutable type")
    @NonAssignableField("Field is final")
    private final Integer integer = new Integer(5);

    @TransitivelyImmutableField("Field has a mutable type")
    @NonAssignableField("Field is final")
    private final TrImmutableClass mutableClass = new TrImmutableClass();

    @TransitivelyImmutableField("concrete object is known")
    @NonAssignableField("The field is final")
    private final TrImmutableClass transitivelyImmutableClass = new TrImmutableClass();

    public Object getTransitivelyImmutableClass() {
        return this.transitivelyImmutableClass;
    }

    private final Object managedObjectManagerLock = new Object();

    @TransitivelyImmutableField("all concrete objects that can be assigned are not known") /////TODO
    private TrImmutableClass fieldWithMutableType = new TrImmutableClass();

    public ConcreteObjectInstanceAssigned(TrImmutableClass transitivelyImmutableClass) {
        this.fieldWithMutableType = transitivelyImmutableClass;
    }
}

class TrImmutableClass {
}
