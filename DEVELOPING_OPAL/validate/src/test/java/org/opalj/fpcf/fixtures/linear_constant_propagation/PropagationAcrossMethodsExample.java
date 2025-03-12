/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.*;

/**
 * An example to test fact and value propagation across methods.
 *
 * @author Robin Körkemeier
 */
public class PropagationAcrossMethodsExample {
    @VariableValues({
            @VariableValue(variable = "param2"),
            @VariableValue(variable = "param3")
    })
    public int linearCalculation1(String msg, int a, int b) {
        System.out.println(msg + ": " + a);
        return 42 - 5 * b;
    }

    public static int linearCalculation2(String msg, int a) {
        System.out.println(msg);
        return 12 - 4 * 4 + a;
    }

    @ConstantValue(variable = "lv3", value = 139)
    public static int linearCalculation3(String msg, int a) {
        System.out.println(msg);
        return a + 11;
    }

    @UnknownValue(variable = "lv3")
    public static int linearCalculation4(String msg, int a) {
        System.out.println(msg);
        return 3 * a;
    }

    @ConstantValues({
            @ConstantValue(variable = "lv5", value = -18),
            @ConstantValue(variable = "lv8", value = 132),
            @ConstantValue(variable = "lva", value = 128),
            @ConstantValue(variable = "lv12", value = 139)
    })
    @VariableValues({
            @VariableValue(variable = "lvd"),
            @VariableValue(variable = "lv10")
    })
    public static void main(String[] args) {
        PropagationAcrossMethodsExample example = new PropagationAcrossMethodsExample();

        int i = example.linearCalculation1("First call", 23, 12);
        int j = example.linearCalculation1("Second call", 2, i);

        int k = linearCalculation2("Third call", j);
        int l = linearCalculation2("Fourth call", args.length);

        int m = example.linearCalculation1("Fifth call", 12, l);

        int n = linearCalculation3("Sixth call", k);

        System.out.println("i: " + i + ", j: " + j + ", k:" + k + ", l: " + l + ", m: " + m + ", n: " + n);
    }
}
