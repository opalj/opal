/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class ForLoop {

    public static void main(String[] args) {
        // Simple for loop with int
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += i;
        }
        System.out.println("Sum of first 10 numbers: " + sum);

        // For loop with an array of integers
        int[] numbers = {1, 2, 3, 4, 5};
        int arraySum = 0;
        for (int i = 0; i < numbers.length; i++) {
            arraySum += numbers[i];
        }
        System.out.println("Sum of array elements: " + arraySum);

        // For loop with a double type
        double product = 1.0;
        for (int i = 1; i <= 5; i++) {
            product *= i * 1.5;
        }
        System.out.println("Product of first 5 numbers multiplied by 1.5: " + product);

        // Nested for loops
        int multiplicationTableSum = 0;
        for (int i = 1; i <= 3; i++) {
            for (int j = 1; j <= 3; j++) {
                multiplicationTableSum += i * j;
            }
        }
        System.out.println("Sum of 3x3 multiplication table: " + multiplicationTableSum);
    }
}
