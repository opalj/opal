/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Parameters_mutation_3 {

    public static void main(String[] args) {
        // Create a class with static fields
        Parameters_mutation_3 myclass = new Parameters_mutation_3();

        // Test with primitive types
        int a = myclass.x; // replaced direct value assignment with an assignment through a new instance
        double b = myclass.y;
        char c = myclass.charValue;

        System.out.println("Sum of int and double: " + sum(a, b));
    }

    // Method with primitive parameters
    public static double sum(int x, double y) {
        return x + y;
    }

    // Static fields
    public static int x = 10;
    public static double y = 20.5;
    public static char charValue = 'A';
}