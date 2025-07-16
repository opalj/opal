/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class WhileLoop_mutation_2 {

    public static void main(String[] args) {
        // Simple while loop
        int counter = 0;
        counter++;
        while (counter < 5) {
            System.out.println("Simple while loop: counter = " + counter);
            counter++;
        }

        // Do-while loop
        int z = 0;
        do {
            System.out.println("Do-while loop: z = " + z);
            z++;
        } while (z < 3);

    }
}