/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.StaticValues;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class StaticFieldNonImmutableExample {
    static int a = 42;
    protected static int b = 23;

    @StaticValues(variableValues = {
            @VariableValue(variable = "a"),
            @VariableValue(variable = "b")
    })
    @VariableValues({
            @VariableValue(variable = "lv0"),
            @VariableValue(variable = "lv1")
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
