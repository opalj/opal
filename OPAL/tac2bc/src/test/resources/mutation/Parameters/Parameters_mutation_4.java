public class Parameters_mutation_4 {

    public static void main(String[] args) {
        // Test with primitive types
        int a = 10;
        double b = 20.5;
        char c = 'A';

        // Create a new instance of Parameters
        Parameters_mutation_4 myclass = new Parameters_mutation_4();

        System.out.println("Sum of int and double: " + sum(a, myclass.myDouble));
    }

    // Method with primitive parameters
    public double myDouble = 0.0;

    public static double sum(int x, double y) {
        return x + y;
    }

}