/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Parameters_mutation_5{

    public static void main(String[] args) {
        // Test with primitive types
        int a = Parameters_mutation_5.fieldInt;
        double b = Parameters_mutation_5.fieldDouble;
        char c = Parameters_mutation_5.fieldChar;

        System.out.println("Sum of int and double: " + sum(a, b));
    }

    // Method with primitive parameters
    public static double sum(int x, double y) {
        return x + y;
    }

    static {
        fieldInt = 10;
        fieldDouble = 20.5;
        fieldChar = 'A';
    }

    static int fieldInt;
    static double fieldDouble;
    static char fieldChar;
}