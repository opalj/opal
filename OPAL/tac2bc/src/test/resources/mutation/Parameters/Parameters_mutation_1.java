/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Parameters_mutation_1 {

    public static void main(String[] args) {
        // Test with primitive types
        int a = 10;
        double b = 20.5;
        char c = 'A';

        double sumResult = sum(a, b);
        System.out.println("Sum of int and double: " + sumResult);
    }

    // Method with primitive parameters
    public static double sum(int x, double y) {
        return x + y;
    }

}