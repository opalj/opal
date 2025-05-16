public class PrimitiveTypeCast_mutation_4 {

    public static void main(String[] args) {
        // Initial values
        int intValue = 100;

        // Perform a calculation
        int calculationResult = intValue * 2;

        //Cast to different types

        // Store the result in a temporary variable
        byte byteValue = (byte) calculationResult;

        // Output the results
        System.out.println("int to byte: " + byteValue);
    }
}