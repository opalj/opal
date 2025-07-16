/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class InstanceField {

    // An instance field
    private int instanceValue;

    public static void main(String[] args) {
        // Create an instance of the class
        InstanceField testInstance = new InstanceField();

        // Set the instance field
        testInstance.setInstanceValue(42);

        // Get the instance field value and print it
        System.out.println("Instance field value: " + testInstance.getInstanceValue());
    }

    // Method to set the instance field
    public void setInstanceValue(int value) {
        this.instanceValue = value;
    }

    // Method to get the instance field value
    public int getInstanceValue() {
        return this.instanceValue;
    }
}
