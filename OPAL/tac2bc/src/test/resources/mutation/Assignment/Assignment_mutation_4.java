/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Assignment_mutation_4 {

    public static void main(String[] args) {
        // Test integer assignment
        int[] a = {10};
        int b = 20;
        int sum = a[0] + b;
        System.out.println("Sum of integers: " + sum);

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
        numbers[0] = firstNumber + 10;
        System.out.println("First number in array after update: " + numbers[0]);
    }
}