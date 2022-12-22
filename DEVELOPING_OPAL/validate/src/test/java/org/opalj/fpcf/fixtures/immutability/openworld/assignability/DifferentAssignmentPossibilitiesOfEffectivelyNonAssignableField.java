/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability;

import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

/**
 * Class encompasses two possible cases of assigning the effectively non assignable field object.
 */
public class DifferentAssignmentPossibilitiesOfEffectivelyNonAssignableField {

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
