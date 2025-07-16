/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.mutation;

public class Assignment_mutation_5 {

    public static void main(String[] args) {
        // Test integer assignment
        int a = 10;
        int b = 20;
        int sum = a + b;
        int result = sum;  // Temp variable to store the result
        System.out.println("Sum of integers: " + result);

        // Test string assignment
        String firstName = "John";
        String lastName = "Doe";
        String fullName = firstName + " " + lastName;
        System.out.println("Full name: " + fullName);

        // Test boolean assignment
        boolean isTrue = true;
        boolean isFalse = false;
        boolean andResult = isTrue && isFalse;
        System.out.println("Boolean AND result: " + andResult);

        // Test array assignment
        int[] numbers = {1, 2, 3};
        int firstNumber = numbers[0];
        int updatedNumber = firstNumber + 10;
        numbers[0] = updatedNumber;
        System.out.println("First number in array after update: " + numbers[0]);
    }
}