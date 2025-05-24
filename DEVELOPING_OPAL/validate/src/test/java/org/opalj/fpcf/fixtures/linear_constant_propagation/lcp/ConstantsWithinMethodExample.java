/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValues;

/**
 * An example to test constants (simple and linear) in a single method.
 *
 * @author Robin Körkemeier
 */
public class ConstantsWithinMethodExample {
    @ConstantValues({
            @ConstantValue(pc = 0, value = 4),
            @ConstantValue(pc = 1, value = 3),
            @ConstantValue(pc = 2, value = 12),
            @ConstantValue(pc = 3, value = 4),
            @ConstantValue(pc = 4, value = 16)
    })
    public static void main(String[] args) {
        int a = 4;
        int b = a;
        int c = 3 * b + 4;

        System.out.println("a: " + a + ", b: " + b + ", c: " + c);
    }
}
