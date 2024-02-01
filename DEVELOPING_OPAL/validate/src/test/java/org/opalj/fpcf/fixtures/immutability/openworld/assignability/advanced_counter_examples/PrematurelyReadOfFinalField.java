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
        System.out.println("Value A.X before constructor:" + PrematurelyReadOfFinalField.n);
        C c = new C();
        System.out.println("Value A.X after constructor:" + PrematurelyReadOfFinalField.n);
        System.out.println("Value C.x after constructor:" + c.x );
    }

}
class B {

    B() {
        PrematurelyReadOfFinalField.n = ((C) this).x;
    }

    void b(C c) {
        PrematurelyReadOfFinalField.n = c.x;
    }

}

class C extends B{

    @AssignableField("Is seen with two different values during construction.")
    public final int x;

    C() {
        super();
        //this.b(this);
        x = 3;
    }
}
