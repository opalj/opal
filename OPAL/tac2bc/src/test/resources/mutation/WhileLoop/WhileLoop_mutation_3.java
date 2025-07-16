/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class WhileLoop_mutation_3 {

    public static void main(String[] args) {
        // Simple while loop
        int[] counterArr = {0};
        while (counterArr[0] < 5) {
            System.out.println("Simple while loop: counter = " + counterArr[0]);
            counterArr[0]++;
        }

        // Do-while loop
        int z = 0;
        do {
            System.out.println("Do-while loop: z = " + z);
            z++;
        } while (z < 3);

    }
}