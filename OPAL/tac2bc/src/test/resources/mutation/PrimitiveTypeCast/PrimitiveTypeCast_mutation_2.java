/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class PrimitiveTypeCast_mutation_2 {

    public static void main(String[] args) {
        // Initial values
        int intValue = 100;

        //Cast to different types

        // int to byte, short, char
        if (true) {
            byte byteValue = (byte) intValue;
        }

        // Output the results
        System.out.println("int to byte: " + (byte) intValue);
    }
}