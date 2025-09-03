/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp.ConstantValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp.VariableValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantArrayElement;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableArrayElement;

/**
 * An example to test reading and writing of arrays at an unknown index.
 *
 * @author Robin Körkemeier
 */
public class ArrayUnknownIndicesExample {
    @ArrayValue(tacIndex = 1, variableElements = {
            @VariableArrayElement(index = 0),
            @VariableArrayElement(index = 1),
            @VariableArrayElement(index = 10),
            @VariableArrayElement(index = 11),
            @VariableArrayElement(index = 12),
            @VariableArrayElement(index = 13),
            @VariableArrayElement(index = 98),
            @VariableArrayElement(index = 99),
    }, constantElements = {
            @ConstantArrayElement(index = 50, value = 99)
    })
    @ConstantValue(tacIndex = 9, value = 0)
    @VariableValue(tacIndex = 15)
    public static void main(String[] args) {
        int[] arr = new int[100];

        int i;
        int j;
        if (args.length == 0) {
            i = 42;
            j = 11;
        } else {
            i = 23;
            j = 12;
        }

        int a1 = arr[i];

        arr[j] = 13;
        arr[50] = 99;

        int a2 = arr[i];

        System.out.println("a1: " + a1 + ", a2: " + a2);
    }
}
