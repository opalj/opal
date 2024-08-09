/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.VariableValue;

public class FieldReadWriteAcrossMethodsExample {
    private int a = -2;

    private void setA(int a) {
        this.a = a;
    }

    private void setATo42() {
        this.a = 42;
    }

    private int getA() {
        return a;
    }

    @ObjectValues({
            @ObjectValue(variable = "lv0", variableValues = {@VariableValue(variable = "a")}),
            @ObjectValue(variable = "lv2", constantValues = {@ConstantValue(variable = "a", value = 42)}),
            @ObjectValue(variable = "lv4", constantValues = {@ConstantValue(variable = "a", value = -2)})
    })
    public static void main(String[] args) {
        FieldReadWriteAcrossMethodsExample example1 = new FieldReadWriteAcrossMethodsExample();
        FieldReadWriteAcrossMethodsExample example2 = new FieldReadWriteAcrossMethodsExample();
        FieldReadWriteAcrossMethodsExample example3 = new FieldReadWriteAcrossMethodsExample();

        example1.setA(23);
        int e1a = example1.getA();

        example2.setATo42();
        int e2a = example2.getA();

        int e3a = example3.getA();

        System.out.println("e1a: " + e1a + ", e2a: " + e2a + ", e3a: " + e3a);
    }
}
