/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantArrayElement;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.VariableArrayElement;

/**
 * An example to test conservative handling of array elements in native method calls.
 *
 * @author Robin Körkemeier
 */
public class ArrayNativeMethodExample {
    @ArrayValues({
            @ArrayValue(pc = 1, variableElements = {
                    @VariableArrayElement(index = 0),
                    @VariableArrayElement(index = 1),
                    @VariableArrayElement(index = 2),
                    @VariableArrayElement(index = 3)
            }),
            @ArrayValue(pc = 15, constantElements = {
                    @ConstantArrayElement(index = 0, value = 42),
                    @ConstantArrayElement(index = 1, value = 23)
            }),
            @ArrayValue(pc = 23, variableElements = {
                    @VariableArrayElement(index = 0),
                    @VariableArrayElement(index = 1),
                    @VariableArrayElement(index = 2),
                    @VariableArrayElement(index = 3),
                    @VariableArrayElement(index = 4),
                    @VariableArrayElement(index = 5)
            })
    })
    public static void main(String[] args) {
        int[] arr1 = new int[]{4, 5, 6, 7};
        int[] arr2 = new int[]{42, 23};
        int[] arr3 = new int[6];
        System.arraycopy(arr1, 1, arr3, 2, 3);

        System.out.println("arr1: {" + arr1[0] + ", " + arr1[1] + ", " + arr1[2] + ", " + arr1[3] + "}; arr2: {" +
                arr2[0] + ", " + arr2[1] + "}; arr3: {" + arr3[0] + ", " + arr3[1] + ", " + arr3[2] + ", " + arr3[3] +
                ", " + arr3[4] + ", " + arr3[5] + "}");
    }
}
