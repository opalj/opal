/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.lcp_on_fields;

import org.opalj.fpcf.properties.lcp_on_fields.*;

public class ArrayReadWriteAcrossMethodsExample {
    public void setIndexTo23(int[] arr, int index) {
        arr[index] = 23;
    }

    public void set11To42(int[] arr) {
        arr[11] = 42;
    }

    @ArrayValues({
            @ArrayValue(variable = "lv3", unknownElements = {
                    @UnknownArrayElement(index = 0),
                    @UnknownArrayElement(index = 1),
                    @UnknownArrayElement(index = 2),
                    @UnknownArrayElement(index = 3)
            }),
            @ArrayValue(variable = "lv5", constantElements = {
                    @ConstantArrayElement(index = 11, value = 42)
            })
    })
    public static void main(String[] args) {
        ArrayReadWriteAcrossMethodsExample example = new ArrayReadWriteAcrossMethodsExample();

        int[] arr1 = new int[100];
        int[] arr2 = new int[100];

        example.setIndexTo23(arr1, 2);
        example.set11To42(arr2);
    }
}
