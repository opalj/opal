/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Array_mutation_4 {

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] array = {10, 20, 30};

        // Print original values
        System.out.println(array[0]);
        System.out.println(array[1]);
        System.out.println(array[2]);

        // Modify and print the second element
        int[] temp = {100};
        array[1] = temp[0];
        System.out.println(array[1]);
    }
}