/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantField;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.UnknownField;

/**
 * An example to test reading and writing fields of objects across methods.
 *
 * @author Robin Körkemeier
 */
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
            @ObjectValue(pc = 0, unknownValues = {@UnknownField(field = "a")}),
            @ObjectValue(pc = 2, constantValues = {@ConstantField(field = "a", value = 42)}),
            @ObjectValue(pc = 4, constantValues = {@ConstantField(field = "a", value = -2)})
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
