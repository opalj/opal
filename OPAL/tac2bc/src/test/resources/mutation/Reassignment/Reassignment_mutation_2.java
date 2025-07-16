/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Reassignment_mutation_2 {

    public static class Myclass {
        public static int field = Integer.MAX_VALUE;
    }

    public static void main(String[] args) {
        int x = 10;
        int y = 5;
        int z = Myclass.field;
        double test = 2.3d;

        System.out.println("Initial values - x: " + x + ", y: " + y + ", z: " + z);

        // Conditional reassignment
        if (-3< 0) {
            x =  Myclass.field + 5;
            System.out.println("After conditional reassignment, x: " + x);
        }
    }
}