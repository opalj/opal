/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability.advanced_counter_examples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * This test case simulates the fact that the `this` object escapes in the constructor before (final) fields
 * are assigned.
 */
public class ThisEscapesDuringConstruction {

    @AssignableField("The this object escapes in the constructor before the field is assigned.")
    final int n;

    public ThisEscapesDuringConstruction() {
        C2.m(this);
        n = 7;
    }
}

class C2 {
    public static void m(ThisEscapesDuringConstruction c) {}
}
