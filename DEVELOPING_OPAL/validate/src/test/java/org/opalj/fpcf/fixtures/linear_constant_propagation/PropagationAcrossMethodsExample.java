/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValues;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class PropagationAcrossMethodsExample {
    public int linearCalculation1(String msg, int a, int b) {
        System.out.println(msg + ": " + a);
        return 42 - 5 * b;
    }

    public static int linearCalculation2(String msg, int a) {
        System.out.println(msg);
        return 12 - 4 * 4 + a;
    }

    @ConstantValues({
            @ConstantValue(variable = "lv5", value = -18),
            @ConstantValue(variable = "lv8", value = 132),
            @ConstantValue(variable = "lva", value = 128)
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

        System.out.println("i: " + i + ", j: " + j + ", k:" + k + ", l: " + l + ", m: " + m);
    }
}
