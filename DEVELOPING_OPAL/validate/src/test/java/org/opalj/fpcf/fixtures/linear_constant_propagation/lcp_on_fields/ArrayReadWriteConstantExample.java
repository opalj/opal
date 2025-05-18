/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.linear_constant_propagation.lcp_on_fields;

import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValue;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ArrayValues;
import org.opalj.fpcf.properties.linear_constant_propagation.lcp_on_fields.ConstantArrayElement;

/**
 * An example to test reading and writing array elements in one method.
 *
 * @author Robin Körkemeier
 */
public class ArrayReadWriteConstantExample {
    @ArrayValues({
            @ArrayValue(variable = "lv1", constantElements = {
                    @ConstantArrayElement(index = 0, value = 0),
                    @ConstantArrayElement(index = 1, value = 0),
                    @ConstantArrayElement(index = 2, value = 42),
                    @ConstantArrayElement(index = 3, value = 4),
                    @ConstantArrayElement(index = 4, value = 0)
            }),
            @ArrayValue(variable = "lv3", constantElements = {
                    @ConstantArrayElement(index = 0, value = 0),
                    @ConstantArrayElement(index = 1, value = 2),
                    @ConstantArrayElement(index = 2, value = 3),
                    @ConstantArrayElement(index = 3, value = 4)
            }),
            @ArrayValue(variable = "lv11", constantElements = {
                    @ConstantArrayElement(index = 0, value = 11),
                    @ConstantArrayElement(index = 1, value = 12),
                    @ConstantArrayElement(index = 2, value = 13)
            })
    })
    public static void main(String[] args) {
        int[] arr1 = new int[5];
        int[] arr2 = new int[]{1, 2, 3, 4};
        int[] arr3 = new int[]{11, 12, 13};

        arr1[2] = 42;
        arr1[3] = arr2[3];
        arr2[0] = arr1[4];

        System.out.println("arr1: {" + arr1[0] + ", " + arr1[1] + ", " + arr1[2] + ", " + arr1[3] + ", " + arr1[4] +
                "}; arr2: {" + arr2[0] + ", " + arr2[1] + ", " + arr2[2] + ", " + arr2[3] + "}; arr3: {" + arr3[0] +
                ", " + arr3[1] + ", " + arr3[2] + "}");
    }
}
