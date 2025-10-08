/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class ArithmeticOperations {

    public static void main(String[] args) {
        // Integer Arithmetic Operations
        int a = 10;
        int b = 5;
        int additionResult = a + b;
        int subtractionResult = a - b;
        int multiplicationResult = a * b;
        int divisionResult = a / b;
        int modulusResult = a % b;

        System.out.println("Integer Addition: " + additionResult);
        System.out.println("Integer Subtraction: " + subtractionResult);
        System.out.println("Integer Multiplication: " + multiplicationResult);
        System.out.println("Integer Division: " + divisionResult);
        System.out.println("Integer Modulus: " + modulusResult);

        // Floating-Point Arithmetic Operations
        double x = 5.5;
        double y = 2.5;
        double additionDoubleResult = x + y;
        double subtractionDoubleResult = x - y;
        double multiplicationDoubleResult = x * y;
        double divisionDoubleResult = x / y;

        System.out.println("Double Addition: " + additionDoubleResult);
        System.out.println("Double Subtraction: " + subtractionDoubleResult);
        System.out.println("Double Multiplication: " + multiplicationDoubleResult);
        System.out.println("Double Division: " + divisionDoubleResult);

        // Mixed Operations (int and double)
        double mixedAdditionResult = a + x;
        double mixedMultiplicationResult = b * y;

        System.out.println("Mixed Addition (int + double): " + mixedAdditionResult);
        System.out.println("Mixed Multiplication (int * double): " + mixedMultiplicationResult);

        // Long Arithmetic Operations
        long p = 100000L;
        long q = 50000L;
        long longAdditionResult = p + q;
        long longSubtractionResult = p - q;
        long longMultiplicationResult = p * q;
        long longDivisionResult = p / q;
        long longModulusResult = p % q;

        System.out.println("Long Addition: " + longAdditionResult);
        System.out.println("Long Subtraction: " + longSubtractionResult);
        System.out.println("Long Multiplication: " + longMultiplicationResult);
        System.out.println("Long Division: " + longDivisionResult);
        System.out.println("Long Modulus: " + longModulusResult);

        // Bitwise Operations with Longs
        long longAndResult = p & q;
        long longOrResult = p | q;
        long longXorResult = p ^ q;

        System.out.println("Long AND: " + longAndResult);
        System.out.println("Long OR: " + longOrResult);
        System.out.println("Long XOR: " + longXorResult);

        // Shift Operations with Longs
        long longShiftLeftResult = p << 2;
        long longShiftRightResult = p >> 2;
        long longUnsignedShiftRightResult = p >>> 2;

        System.out.println("Long Shift Left: " + longShiftLeftResult);
        System.out.println("Long Shift Right: " + longShiftRightResult);
        System.out.println("Long Unsigned Shift Right: " + longUnsignedShiftRightResult);

        // Short and Byte Operations
        short s1 = 10;
        short s2 = 20;
        byte b1 = 2;
        byte b2 = 3;

        int shortAdditionResult = s1 + s2;
        int byteAdditionResult = b1 + b2;

        System.out.println("Short Addition: " + shortAdditionResult);
        System.out.println("Byte Addition: " + byteAdditionResult);
    }
}
