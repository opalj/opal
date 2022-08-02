/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.open_world.assignability;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * Class encompasses two possible cases of assigning the effectively non assignable field o.
 */
@MutableType("Class is not final")
@NonTransitivelyImmutableClass("Class has only a transitively immutable field")
public class DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField {

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("Field is only once assigned in the constructor via new created object or parameter")
    private Object object;

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField() {
        this.object = new Object();
    }

    public DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField(int n) {
        this.object = new Integer(n);
    }

    public Object getObject(){
        return this.object;
    }
}
