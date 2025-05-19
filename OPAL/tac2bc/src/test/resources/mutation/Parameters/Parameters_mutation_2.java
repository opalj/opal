/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Parameters_mutation_2 {

    public static void main(String[] args) {
        // Test with primitive types
        int[] aArray = {10};
        double b = 20.5;
        char c = 'A';

        System.out.println("Sum of int and double: " + sum(aArray[0], b));
    }

    // Method with primitive parameters
    public static double sum(int x, double y) {
        return x + y;
    }
}