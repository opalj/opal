/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class InstanceField {

    private int instanceValue;

    public static void main(String[] args) {
        InstanceField testInstance = new InstanceField();
        testInstance.instanceValue = 42;
        System.out.println("Instance field value: " + testInstance.instanceValue);
    }
}
