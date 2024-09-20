/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability.advanced_counter_examples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * The default value of the field x is assigned to another field n during construction and as
 * a result seen with two different values.
 */
public class PrematurelyReadOfFinalField {

    @AssignableField("Field n is assigned with different values.")
    static int n = 5;

    public static void main(String[] args) {
        C c = new C();
    }

}
class B {
    B() {
        PrematurelyReadOfFinalField.n = ((C) this).x;
    }
}

class C extends B{

    @AssignableField("Is seen with two different values during construction.")
    public final int x;

    C() {
        super();
        x = 3;
    }
}
