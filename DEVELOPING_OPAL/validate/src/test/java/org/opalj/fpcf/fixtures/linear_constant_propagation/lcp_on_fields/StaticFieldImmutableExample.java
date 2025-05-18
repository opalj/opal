/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.StaticValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;

/**
 * An example to test detection of static immutable fields.
 *
 * @author Robin Körkemeier
 */
public final class StaticFieldImmutableExample {
    protected static int a = 42;
    static int b;
    private static int c;
    private static int d;
    static final int e = 2;

    static {
        b = 23;

        if (System.out != null) {
            c = 11;
        } else {
            c = 12;
        }
    }

    @StaticValues(constantValues = {
            @ConstantValue(variable = "a", value = 42),
            @ConstantValue(variable = "b", value = 23),
            @ConstantValue(variable = "d", value = 0)
    }, variableValues = {
            @VariableValue(variable = "c"),
            @VariableValue(variable = "e")
    })
    @ConstantValues({
            @ConstantValue(variable = "lv0", value = 42),
            @ConstantValue(variable = "lv1", value = 23),
            @ConstantValue(variable = "lv3", value = 0),
            @ConstantValue(variable = "lv4", value = 2)
    })
    @VariableValue(variable = "lv2")
    public static void main(String[] args) {
        int a = StaticFieldImmutableExample.a;
        int b = StaticFieldImmutableExample.b;
        int c = StaticFieldImmutableExample.c;
        int d = StaticFieldImmutableExample.d;
        int e = StaticFieldImmutableExample.e;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c + ", d: " + d + ", e: " + e);
    }
}
