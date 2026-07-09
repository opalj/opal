/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability.advanced_counter_examples;

import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

/**
 * The value of the field x is read with its default value (0)
 * in the constructor before assignment and assigned to a public field.
 * Thus, the value can be accessed from everywhere.
 */
public class ValueReadBeforeAssignment {
    @AssignableField("Field value is read before assignment.")
    private int x;
    @AssignableField("Field y is public and not final.")
    public int y;

    public ValueReadBeforeAssignment() {
        y = x;
        x = 42;
    }

    public ValueReadBeforeAssignment foo() {
        return new ValueReadBeforeAssignment();
    }
}
