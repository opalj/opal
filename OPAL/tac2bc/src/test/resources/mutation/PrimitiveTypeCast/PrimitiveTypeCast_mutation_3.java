/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class PrimitiveTypeCast_mutation_3 {

    public static void main(String[] args) {
        // Initial values
        int[] intValueArr = {100};

        //Cast to different types

        // int to byte, short, char
        byte byteValue = (byte) intValueArr[0];

        // Output the results
        System.out.println("int to byte: " + byteValue);
    }
}