/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class ArithmeticOperations_mutation_4 {
    public static void main(String[] args) {
        int a = 10;
        int b = 5;
        int product = a * b;
        int sum = a + b;
        int difference = a - b;
        int quotient = a / b;

        System.out.println("Sum: " + sum);
        System.out.println("Difference: " + difference);
        System.out.println("Product: " + product);
        System.out.println("Quotient: " + quotient);
    }
}