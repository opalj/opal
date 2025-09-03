/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValues;

/**
 * An example to test field accesses are detected by classical linear constant propagation.
 *
 * @author Robin Körkemeier
 */
public class FieldAccessExample {
    private final int a;
    int b;
    static int c = 42;
    int[] d = new int[]{23};

    public FieldAccessExample(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    @VariableValues({
            @VariableValue(tacIndex = 4),
            @VariableValue(tacIndex = 5),
            @VariableValue(tacIndex = 6),
            @VariableValue(tacIndex = 8),
            @VariableValue(tacIndex = 11)
    })
    public static void main(String[] args) {
        FieldAccessExample example = new FieldAccessExample(11, 22);

        int i = example.getA();
        int j = example.b;
        int k = c;
        int l = example.d.length;
        int m = example.d[0];

        System.out.println("i: " + i + ", j: " + j + ", k: " + k + ", l: " + l + ", m: " + m);
    }
}
