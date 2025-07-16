/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class ForLoop_mutation_4 {

    public static void main(String[] args) {
        // Simple for loop with int
        int[] sumArray = {0};
        int sum = sumArray[0];
        for (int i = 0; i < 5; i++) {
            sum += i;
        }
        System.out.println("Sum of first 5 numbers: " + sum);

        // For loop with an array of integers
        int[] numbers = {1, 2, 3};
        int arraySum = 0;
        for (int i = 0; i < numbers.length; i++) {
            arraySum += numbers[i];
        }
        System.out.println("Sum of array elements: " + arraySum);

        // For loop with a double type
        double[] productArray = {1.0};
        double product = productArray[0];
        for (int i = 1; i <= 3; i++) {
            product *= i * 1.5;
        }
        System.out.println("Product of first 3 numbers multiplied by 1.5: " + product);
    }
}