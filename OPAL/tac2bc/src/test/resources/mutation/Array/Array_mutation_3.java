/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Array_mutation_3 {

    public static int myStaticField = 0;

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] array = {10, 20, 30};

        // Print original values
        System.out.println(array[0]);
        System.out.println(array[1]);
        System.out.println(array[2]);

        // Modify and print the second element
        array[1] = myStaticField + 100;
        System.out.println(array[1]);
    }
}