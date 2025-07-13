/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantField;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.StaticValues;

/**
 * An example to test reading and writing private static fields across methods.
 *
 * @author Robin Körkemeier
 */
public class StaticFieldReadWriteAcrossMethodsExample {
    private static int a;

    public void setATo11() {
        a = 11;
    }

    private void setATo42() {
        a = 42;
    }

    @StaticValues(constantValues = {
            @ConstantField(field = "a", value = 42)
    })
    @ConstantValues({
            @ConstantValue(tacIndex = 3, value = 11),
            @ConstantValue(tacIndex = 5, value = 42)
    })
    public static void main(String[] args) {
        StaticFieldReadWriteAcrossMethodsExample example = new StaticFieldReadWriteAcrossMethodsExample();

        example.setATo11();

        int a1 = a;

        example.setATo42();

        int a2 = a;

        System.out.println("a1: " + a1 + ", a2: " + a2);
    }
}
