/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Reassignment_mutation_1 {

    public static void main(String[] args) {
        int x = 10;
        int y = 5;
        int z = Integer.MAX_VALUE;
        double test = 2.3d;

        System.out.println("Initial values - x: " + x + ", y: " + y + ", z: " + z);

        // Conditional reassignment
        int[] arr = {Integer.MAX_VALUE+5};
        if (-3< 0) {
            x = arr[0];
            System.out.println("After conditional reassignment, x: " + x);
        }
    }
}