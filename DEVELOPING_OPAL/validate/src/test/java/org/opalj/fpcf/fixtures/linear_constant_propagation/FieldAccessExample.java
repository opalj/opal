package org.opalj.fpcf.fixtures.linear_constant_propagation;

import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValues;

public class FieldAccessExample {
    private final int a;
    int b;
    static int c = 42;

    public FieldAccessExample(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    @VariableValues({
            @VariableValue(variable = "lv4"),
            @VariableValue(variable = "lv5"),
            @VariableValue(variable = "lv6")
    })
    public static void main(String[] args) {
        FieldAccessExample example = new FieldAccessExample(11, 22);

        int i = example.getA();
        int j = example.b;
        int k = c;

        System.out.println("i: " + i + ", j: " + j + ", k: " + k);
    }
}
