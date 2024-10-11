/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class StaticFieldNonImmutableExample {
    private static int a = 11;
    static int b = 42;
    protected static int c = 23;

    @VariableValues({
            @VariableValue(variable = "lv2"),
            @VariableValue(variable = "lv3"),
            @VariableValue(variable = "lv4")
    })
    public static void main(String[] args) {
        a = 12;

        int a = StaticFieldNonImmutableExample.a;
        int b = StaticFieldNonImmutableExample.b;
        int c = StaticFieldNonImmutableExample.c;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c);
    }
}

class StaticFieldNonImmutable2Example {
    void foobar() {
        StaticFieldNonImmutableExample.b = 23;
    }
}
