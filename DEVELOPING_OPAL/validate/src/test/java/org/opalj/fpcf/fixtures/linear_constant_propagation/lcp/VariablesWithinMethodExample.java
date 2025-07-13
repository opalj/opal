/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues;

/**
 * An example to test detection of variable values within a method.
 *
 * @author Robin Körkemeier
 */
public class VariablesWithinMethodExample {
    @VariableValues({
            @VariableValue(tacIndex = 0),
            @VariableValue(tacIndex = 3),
            @VariableValue(tacIndex = 6)
    })
    public static void main(String[] args) {
        int a = args.length;
        int b = args[0].length();
        int c = Integer.valueOf(42).hashCode();

        System.out.println("a: " + a + ", b: " + b + ", c: " + c);
    }
}
