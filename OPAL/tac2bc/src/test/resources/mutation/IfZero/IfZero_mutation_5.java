/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class IfZero_mutation_5 {

    public static void main(String[] args) {
        // Test with a positive number, zero, and a negative number
        int[] testValues = {10, 0, -5};

        for (int value : testValues) {
            compareWithZero(value);
        }
    }

    public static void compareWithZero(int k) {
        if (k == 0) {
            if (true) {
                System.out.println("k is equal to zero.");
            }
        } else if (k > 0) {
            System.out.println("k is greater than zero.");
        } else {
            System.out.println("k is less than zero.");
        }
    }
}