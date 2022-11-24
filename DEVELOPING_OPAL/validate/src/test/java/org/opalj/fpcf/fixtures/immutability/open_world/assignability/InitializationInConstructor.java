package org.opalj.fpcf.fixtures.immutability.open_world.assignability;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

/*
 * This class encompasses two different cases of fields assigned in the constructor.
 */
class InitializationInConstructorAssignable {

    @AssignableField("The field is written everytime it is passed to the constructor of another instance.")
    private InitializationInConstructorAssignable child;
    public InitializationInConstructorAssignable(InitializationInConstructorAssignable parent) {
        parent.child = this;
        }
    }

class InitializationInConstructorNonAssignable {

    @EffectivelyNonAssignableField("The field is only assigned once in its own constructor.")
    private InitializationInConstructorNonAssignable parent;
    public InitializationInConstructorNonAssignable(InitializationInConstructorNonAssignable parent) {
        this.parent = parent.parent;
    }
}
