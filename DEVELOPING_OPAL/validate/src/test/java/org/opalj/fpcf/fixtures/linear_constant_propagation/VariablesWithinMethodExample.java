/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class VariablesWithinMethodExample {
    @VariableValues({
            @VariableValue(variable = "lv0"),
            @VariableValue(variable = "lv3"),
            @VariableValue(variable = "lv6")
    })
    public static void main(String[] args) {
        int a = args.length;
        int b = args[0].length();
        int c = Integer.valueOf(42).hashCode();

        System.out.println("a: " + a + ", b: " + b + ", c: " + c);
    }
}
