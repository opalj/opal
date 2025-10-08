/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class Parameters {

    public static void main(String[] args) {
        // Test with primitive types
        int a = 10;
        double b = 20.5;
        char c = 'A';

        System.out.println("Sum of int and double: " + sum(a, b));
        System.out.println("Character to int: " + charToInt(c));

        // Test with array parameters
        int[] numbers = {1, 2, 3, 4, 5};
        System.out.println("Sum of array elements: " + sumArray(numbers));

        // Test with varargs
        System.out.println("Sum with varargs: " + sumVarArgs(1, 2, 3, 4, 5));

        // Test with multiple primitive parameters
        System.out.println("Average of three numbers: " + average(5, 10, 15));
    }

    // Method with primitive parameters
    public static double sum(int x, double y) {
        return x + y;
    }

    // Method with a char parameter
    public static int charToInt(char ch) {
        return ch;
    }

    // Method with an array parameter
    public static int sumArray(int[] array) {
        int sum = 0;
        for (int num : array) {
            sum += num;
        }
        return sum;
    }

    // Method with varargs parameter
    public static int sumVarArgs(int... numbers) {
        int sum = 0;
        for (int num : numbers) {
            sum += num;
        }
        return sum;
    }

    // Method with multiple primitive parameters
    public static double average(int x, int y, int z) {
        return (x + y + z) / 3.0;
    }
}
