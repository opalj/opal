/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class encompasses two cases of assigning the non-assignable field object.
 */
@MutableType("Class is not final.")
@NonTransitivelyImmutableClass("Class has only a transitively immutable field.")
public class DifferentAssignmentPossibilitiesOfNonAssignableField {

    @NonTransitivelyImmutableField("")
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