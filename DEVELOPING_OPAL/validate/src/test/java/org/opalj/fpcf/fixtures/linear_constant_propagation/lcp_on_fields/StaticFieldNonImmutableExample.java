/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.StaticValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableField;

/**
 * An example to test detection of static but non-immutable fields.
 *
 * @author Robin Körkemeier
 */
public class StaticFieldNonImmutableExample {
    static int a = 42;
    protected static int b = 23;

    @StaticValues(variableValues = {
            @VariableField(field = "a"),
            @VariableField(field = "b")
    })
    @VariableValues({
            @VariableValue(tacIndex = 0),
            @VariableValue(tacIndex = 1)
    })
    public static void main(String[] args) {
        int a = StaticFieldNonImmutableExample.a;
        int b = StaticFieldNonImmutableExample.b;

        System.out.println("a: " + a + ", b: " + b);
    }
}

class StaticFieldNonImmutable2Example {
    void foobar() {
        StaticFieldNonImmutableExample.a = 23;
    }
}
