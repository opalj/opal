/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class BranchingExample {
    @ConstantValue(variable = "lva", value = 8)
    @VariableValues({
            @VariableValue(variable = "lv2"),
            @VariableValue(variable = "lv8")
    })
    public static void main(String[] args) {
        int a = 23;
        int b = 7;

        if (args.length == 0) {
            a = 42;
            b = 6;
            b++;
        }

        int c = 1 + a;
        int d = b + 1;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c + ", d: " + d);
    }
}
