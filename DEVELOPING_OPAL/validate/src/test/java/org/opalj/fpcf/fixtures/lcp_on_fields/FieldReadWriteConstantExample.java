/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;

/**
 * An example to test reading and writing of object fields with constants.
 *
 * @author Robin Körkemeier
 */
public class FieldReadWriteConstantExample {
    private int a = -1;

    @ObjectValues({
            @ObjectValue(variable = "lv0", constantValues = {@ConstantValue(variable = "a", value = -1)}),
            @ObjectValue(variable = "lv2", constantValues = {@ConstantValue(variable = "a", value = 42)}),
            @ObjectValue(variable = "lv4", constantValues = {@ConstantValue(variable = "a", value = 41)})
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
