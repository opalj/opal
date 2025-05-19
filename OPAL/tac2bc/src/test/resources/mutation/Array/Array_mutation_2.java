/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Array_mutation_2 {

    public static void main(String[] args) {
        // Single-Dimensional Array
        int[] array = {10, 20, 30};

        // Print original values
        System.out.println(array[0]);
        System.out.println(array[1]);
        System.out.println(array[2]);

        // Store the result of a calculation in a temporary variable
        int modifiedValue = array[1];

        // Modify and print the second element
        modifiedValue = 100;
        System.out.println(modifiedValue);
    }
}