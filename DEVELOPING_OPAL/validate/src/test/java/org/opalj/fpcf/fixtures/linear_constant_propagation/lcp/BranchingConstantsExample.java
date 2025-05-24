/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues;

/**
 * An example to test simple variable values in presence of if-then-else constructs.
 *
 * @author Robin Körkemeier
 */
public class BranchingConstantsExample {
    @ConstantValue(pc = 13, value = 8)
    @VariableValues({
            @VariableValue(pc = 2),
            @VariableValue(pc = 11),
            @VariableValue(pc = 15)
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
