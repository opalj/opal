/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

class ArbitraryCallInConstructor {

    @AssignableField("The presence of arbitrary calls prevents guaranteeing that no read-write paths exist.")
    private boolean value;

    public ArbitraryCallInConstructor(boolean v) {
        this.arbitraryCallee();
        this.value = v;
    }

    public void arbitraryCallee() {}
}

class GetClassInConstructor {

    @EffectivelyNonAssignableField("The field is only assigned once in its own constructor.")
    private boolean value;

    public GetClassInConstructor(boolean v) {
        System.out.println(this.getClass());
        this.value = v;
    }
}
