/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.ObjectValue;
import org.opalj.fpcf.properties.lcp_on_fields.ObjectValues;
import org.opalj.fpcf.properties.lcp_on_fields.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.ConstantValue;

/**
 * An example to test conservative handling of objects when encountering native methods.
 *
 * @author Robin Körkemeier
 */
public class ObjectNativeMethodExample {
    int a = 2;

    @VariableValue(variable = "lv0")
    @ObjectValues({
            @ObjectValue(variable = "lv2", constantValues = {
                    @ConstantValue(variable = "a", value = 2)
            })
    })
    public static void main(String[] args) {
        ObjectNativeMethodExample example1 = new ObjectNativeMethodExample();
        ObjectNativeMethodExample example2 = new ObjectNativeMethodExample();

        Class<?> clazz = example1.getClass();

        System.out.println("example1.a: " + example1.a + ", example2.a: " + example2.a + ", clazz: " + clazz.getName());
    }
}
