/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.assignability;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class encompasses two cases of assigning non assignable fields.
 */
@MutableType("Class is not final.")
@TransitivelyImmutableClass("Class has only a transitively immutable field.")
public class DifferentAssignmentPossibilitiesOfNonAssignableField {

    @TransitivelyImmutableField("Field is non assignable and has a transitively immutable type.")
    @NonAssignableField("Field is final")
    private final Object object;

    public DifferentAssignmentPossibilitiesOfNonAssignableField(int n) {
        this.object = new Integer(n);
    }

    public DifferentAssignmentPossibilitiesOfNonAssignableField() {
        this.object = new Object();
    }

    public Object getObject(){
        return this.object;
    }
}