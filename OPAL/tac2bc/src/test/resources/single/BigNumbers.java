/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class BigNumbers {

    public static void main(String[] args) {
        // Testing with Integer.MAX_VALUE and Integer.MIN_VALUE
        int maxValueInt = Integer.MAX_VALUE;
        int minValueInt = Integer.MIN_VALUE;
        int largeValueInt = 1_000_000_000;
        int largeNegativeValueInt = -1_000_000_000;

        // Testing with Long.MAX_VALUE and Long.MIN_VALUE
        long maxValueLong = Long.MAX_VALUE;
        long minValueLong = Long.MIN_VALUE;
        long largeValueLong = 1_000_000_000_000L;
        long largeNegativeValueLong = -1_000_000_000_000L;

        // Testing with Double.MAX_VALUE and Double.MIN_VALUE
        double maxValueDouble = Double.MAX_VALUE;
        double minValueDouble = Double.MIN_VALUE;
        double largeValueDouble = 1e100;
        double largeNegativeValueDouble = -1e100;

        // Testing with Float.MAX_VALUE and Float.MIN_VALUE
        float maxValueFloat = Float.MAX_VALUE;
        float minValueFloat = Float.MIN_VALUE;
        float largeValueFloat = 1e30f;
        float largeNegativeValueFloat = -1e30f;

        // Arithmetic operations for int
        int sumInt = maxValueInt + largeValueInt;         // May cause overflow
        int diffInt = minValueInt - largeValueInt;        // May cause underflow
        int productInt = maxValueInt * 2;                 // Likely overflow
        int quotientInt = maxValueInt / 2;                // Safe operation
        int remainderInt = maxValueInt % largeValueInt;   // Safe operation

        // Arithmetic operations for long
        long sumLong = maxValueLong + largeValueLong;     // May cause overflow
        long diffLong = minValueLong - largeValueLong;    // May cause underflow
        long productLong = maxValueLong * 2;              // Likely overflow
        long quotientLong = maxValueLong / 2;             // Safe operation
        long remainderLong = maxValueLong % largeValueLong; // Safe operation

        // Arithmetic operations for double
        double sumDouble = maxValueDouble + largeValueDouble;       // Large sum
        double diffDouble = minValueDouble - largeValueDouble;      // Small difference
        double productDouble = maxValueDouble * 2;                  // Large product
        double quotientDouble = maxValueDouble / 2;                 // Safe operation
        double remainderDouble = maxValueDouble % largeValueDouble; // Remainder

        // Arithmetic operations for float
        float sumFloat = maxValueFloat + largeValueFloat;           // Large sum
        float diffFloat = minValueFloat - largeValueFloat;          // Small difference
        float productFloat = maxValueFloat * 2;                     // Large product
        float quotientFloat = maxValueFloat / 2;                    // Safe operation
        float remainderFloat = maxValueFloat % largeValueFloat;     // Remainder

        // Print results for int
        System.out.println("Int Sum (maxValue + largeValue): " + sumInt);
        System.out.println("Int Difference (minValue - largeValue): " + diffInt);
        System.out.println("Int Product (maxValue * 2): " + productInt);
        System.out.println("Int Quotient (maxValue / 2): " + quotientInt);
        System.out.println("Int Remainder (maxValue % largeValue): " + remainderInt);

        // Print results for long
        System.out.println("Long Sum (maxValue + largeValue): " + sumLong);
        System.out.println("Long Difference (minValue - largeValue): " + diffLong);
        System.out.println("Long Product (maxValue * 2): " + productLong);
        System.out.println("Long Quotient (maxValue / 2): " + quotientLong);
        System.out.println("Long Remainder (maxValue % largeValue): " + remainderLong);

        // Print results for double
        System.out.println("Double Sum (maxValue + largeValue): " + sumDouble);
        System.out.println("Double Difference (minValue - largeValue): " + diffDouble);
        System.out.println("Double Product (maxValue * 2): " + productDouble);
        System.out.println("Double Quotient (maxValue / 2): " + quotientDouble);
        System.out.println("Double Remainder (maxValue % largeValue): " + remainderDouble);

        // Print results for float
        System.out.println("Float Sum (maxValue + largeValue): " + sumFloat);
        System.out.println("Float Difference (minValue - largeValue): " + diffFloat);
        System.out.println("Float Product (maxValue * 2): " + productFloat);
        System.out.println("Float Quotient (maxValue / 2): " + quotientFloat);
        System.out.println("Float Remainder (maxValue % largeValue): " + remainderFloat);

        // Comparisons for int
        boolean isMaxGreaterThanMinInt = maxValueInt > minValueInt;
        boolean isLargeValueGreaterThanMaxInt = largeValueInt > maxValueInt;
        boolean isMinLessThanLargeNegativeValueInt = minValueInt < largeNegativeValueInt;
        boolean isEqualInt = maxValueInt == Integer.MAX_VALUE;

        // Print comparison results for int
        System.out.println("Is maxValue > minValue? (int) " + isMaxGreaterThanMinInt);
        System.out.println("Is largeValue > maxValue? (int) " + isLargeValueGreaterThanMaxInt);
        System.out.println("Is minValue < largeNegativeValue? (int) " + isMinLessThanLargeNegativeValueInt);
        System.out.println("Is maxValue equal to Integer.MAX_VALUE? (int) " + isEqualInt);

        // Comparisons for long
        boolean isMaxGreaterThanMinLong = maxValueLong > minValueLong;
        boolean isLargeValueGreaterThanMaxLong = largeValueLong > maxValueLong;
        boolean isMinLessThanLargeNegativeValueLong = minValueLong < largeNegativeValueLong;
        boolean isEqualLong = maxValueLong == Long.MAX_VALUE;

        // Print comparison results for long
        System.out.println("Is maxValue > minValue? (long) " + isMaxGreaterThanMinLong);
        System.out.println("Is largeValue > maxValue? (long) " + isLargeValueGreaterThanMaxLong);
        System.out.println("Is minValue < largeNegativeValue? (long) " + isMinLessThanLargeNegativeValueLong);
        System.out.println("Is maxValue equal to Long.MAX_VALUE? (long) " + isEqualLong);

        // Comparisons for double
        boolean isMaxGreaterThanMinDouble = maxValueDouble > minValueDouble;
        boolean isLargeValueGreaterThanMaxDouble = largeValueDouble > maxValueDouble;
        boolean isMinLessThanLargeNegativeValueDouble = minValueDouble < largeNegativeValueDouble;
        boolean isEqualDouble = maxValueDouble == Double.MAX_VALUE;

        // Print comparison results for double
        System.out.println("Is maxValue > minValue? (double) " + isMaxGreaterThanMinDouble);
        System.out.println("Is largeValue > maxValue? (double) " + isLargeValueGreaterThanMaxDouble);
        System.out.println("Is minValue < largeNegativeValue? (double) " + isMinLessThanLargeNegativeValueDouble);
        System.out.println("Is maxValue equal to Double.MAX_VALUE? (double) " + isEqualDouble);

        // Comparisons for float
        boolean isMaxGreaterThanMinFloat = maxValueFloat > minValueFloat;
        boolean isLargeValueGreaterThanMaxFloat = largeValueFloat > maxValueFloat;
        boolean isMinLessThanLargeNegativeValueFloat = minValueFloat < largeNegativeValueFloat;
        boolean isEqualFloat = maxValueFloat == Float.MAX_VALUE;

        // Print comparison results for float
        System.out.println("Is maxValue > minValue? (float) " + isMaxGreaterThanMinFloat);
        System.out.println("Is largeValue > maxValue? (float) " + isLargeValueGreaterThanMaxFloat);
        System.out.println("Is minValue < largeNegativeValue? (float) " + isMinLessThanLargeNegativeValueFloat);
        System.out.println("Is maxValue equal to Float.MAX_VALUE? (float) " + isEqualFloat);
    }
}
