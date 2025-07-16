/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class MethodCall {

    public static void main(String[] args) {
        // Instance method call
        MethodCall instance = new MethodCall();
        int instanceResult = instance.instanceMethod(5, 3);
        System.out.println("Instance method result: " + instanceResult);
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }
}
