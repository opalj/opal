public class PrimitiveTypeCast_mutation_1 {

    public static void main(String[] args) {
        // Initial values
        int intValue = 100;

        // Calculate the result
        int intermediateValue = intValue - 50;

        //Cast to different types

        // int to byte, short, char
        byte byteValue = (byte) intermediateValue;

        // Output the results
        System.out.println("int to byte: " + byteValue);
    }
}