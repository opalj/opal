/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantField;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ObjectValues;

/**
 * An example to test reading and writing of object fields with constants.
 *
 * @author Robin Körkemeier
 */
public class FieldReadWriteConstantExample {
    private int a = -1;

    @ObjectValues({
            @ObjectValue(pc = 0, constantValues = {@ConstantField(field = "a", value = -1)}),
            @ObjectValue(pc = 2, constantValues = {@ConstantField(field = "a", value = 42)}),
            @ObjectValue(pc = 4, constantValues = {@ConstantField(field = "a", value = 41)})
    })
    public static void main(String[] args) {
        FieldReadWriteConstantExample example1 = new FieldReadWriteConstantExample();
        FieldReadWriteConstantExample example2 = new FieldReadWriteConstantExample();
        FieldReadWriteConstantExample example3 = new FieldReadWriteConstantExample();

        example2.a = 23;
        example2.a = 42;

        example3.a = example2.a;
        example3.a--;

        System.out.println("e1: " + example1.a + ", e2: " + example2.a + ", e3: " + example3.a);
    }
}
