/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Array_mutation_5 {

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] array = {10, 20, 30};

        // Print original values
        System.out.println(array[0]);
        System.out.println(array[1]);
        System.out.println(array[2]);

        if (true) {
            array[1] = 100;
        }
        System.out.println(array[1]);
    }
}