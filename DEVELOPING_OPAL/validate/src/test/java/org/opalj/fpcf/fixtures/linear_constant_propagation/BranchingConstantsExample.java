/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

/**
 * An example to test simple variable values in presence of if-then-else constructs.
 *
 * @author Robin Körkemeier
 */
public class BranchingConstantsExample {
    @ConstantValue(variable = "lvd", value = 8)
    @VariableValues({
            @VariableValue(variable = "lv2"),
            @VariableValue(variable = "lvb"),
            @VariableValue(variable = "lvf")
    })
    public static void main(String[] args) {
        int a = 23;
        int b = 7;

        int c;
        if (args.length == 0) {
            a = 42;
            b = 6;
            b++;
            c = 1;
        } else {
            c = 2;
        }

        int d = 1 + a;
        int e = b + 1;
        int f = 1 - c;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c + ", d: " + d + ", e: " + e + ", f: " + f);
    }
}
