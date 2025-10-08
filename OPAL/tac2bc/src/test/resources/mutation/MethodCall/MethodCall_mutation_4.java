/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class MethodCall_mutation_4 {

    public static void main(String[] args) {
        // Instance method call with parameters swapped
        MethodCall_mutation_4 instance = new MethodCall_mutation_4();
        int instanceResult = instance.instanceMethod(3, 5); // Parameters swapped
        System.out.println("Instance method result: " + instanceResult);
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }
}
