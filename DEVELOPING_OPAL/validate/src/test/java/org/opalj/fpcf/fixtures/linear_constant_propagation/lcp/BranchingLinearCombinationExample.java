/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues;

/**
 * An example to test linear combination values in the presence of if-then-else constructs.
 *
 * @author Robin Körkemeier
 */
public class BranchingLinearCombinationExample {
    private static int linearCalculation1(int y, int x) {
        int z;
        if (y > 0) {
            z = 2 * x + 4;
        } else {
            z = 3 * x - 3;
        }
        return z;
    }

    private static int linearCalculation2(int y, int x) {
        if (y < 0) {
            return 4 * x - 13;
        } else {
            return x + 2;
        }
    }

    @ConstantValues({
            @ConstantValue(tacIndex = 18, value = 19),
            @ConstantValue(tacIndex = 20, value = 18),
            @ConstantValue(tacIndex = 27, value = 7)
    })
    @VariableValues({
            @VariableValue(tacIndex = 23),
            @VariableValue(tacIndex = 30)
    })
    public static void main(String[] args) {
        int a = 7;

        if (args.length == 0) {
            a = 6;
            a++;
        }

        int b;
        if (args.length == 1) {
            b = 2 * a + 6;
        } else {
            b = 3 * a - 1;
        }

        int c = b - 1;

        int d = linearCalculation1(args.length, a);
        int e = linearCalculation1(args.length, 4);
        int f = linearCalculation2(args.length, a - 2);
        int g = linearCalculation2(args.length, 2);

        System.out.println("a: " + a + ", b: " + b + ", c: " + c + ", d: " + d + ", e: " + e + ", f: " + f + ", g: " + g);
    }
}
