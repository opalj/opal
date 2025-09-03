/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;

/**
 * An example to test behavior of IDE solver when encountering recursion.
 *
 * @author Robin Körkemeier
 */
public class RecursionExample {
    public static int recursive1(int a) {
        if (a > 0) {
            a -= 2;
            System.out.println(recursive1(a));
            a += 2;
        }

        return a + 3;
    }

    @ConstantValue(tacIndex = 1, value = 14)
    public static void main(String[] args) {
        int i = recursive1(11);

        System.out.println("i: " + i);
    }
}
