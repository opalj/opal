/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class PrimitiveTypeCast {

    public static void main(String[] args) {
        // Initial values
        int intValue = 100;
        double doubleValue = 123.456;
        long longValue = 9876543210L;
        float floatValue = 3.14f;

        //Cast to different types

        // int to byte, short, char
        byte byteValue = (byte) intValue;
        short shortValue = (short) intValue;
        char charValue = (char) intValue;

        // long to int, float, double
        int intFromLong = (int) longValue;
        float floatFromLong = (float) longValue;
        double doubleFromLong = (double) longValue;

        // double to int, long, float
        int intFromDouble = (int) doubleValue;
        long longFromDouble = (long) doubleValue;
        float floatFromDouble = (float) doubleValue;

        // float to int, long, double
        int intFromFloat = (int) floatValue;
        long longFromFloat = (long) floatValue;
        double doubleFromFloat = (double) floatValue;

        // Output the results
        System.out.println("int to byte: " + byteValue);
        System.out.println("int to short: " + shortValue);
        System.out.println("int to char: " + (int) charValue); // Print char as int to show ASCII value
        System.out.println("long to int: " + intFromLong);
        System.out.println("long to float: " + floatFromLong);
        System.out.println("long to double: " + doubleFromLong);
        System.out.println("double to int: " + intFromDouble);
        System.out.println("double to long: " + longFromDouble);
        System.out.println("double to float: " + floatFromDouble);
        System.out.println("float to int: " + intFromFloat);
        System.out.println("float to long: " + longFromFloat);
        System.out.println("float to double: " + doubleFromFloat);
    }
}
