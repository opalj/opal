public class Array_mutation_1 {

    // Declare a static field
    private static final int INITIAL_VALUE = 20;

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] array = {10, INITIAL_VALUE, 30};

        // Print original values
        System.out.println(array[0]);
        System.out.println(array[1]);
        System.out.println(array[2]);

        // Modify and print the second element
        array[1] = 100;
        System.out.println(array[1]);
    }
}