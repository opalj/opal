/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class Compare {

    public static void main(String[] args) {
        // Test case 1: Float comparison
        float f1 = 10.5f;
        float f2 = 20.5f;

        int floatComparisonResult;
        if (f1 > f2) {
            floatComparisonResult = 1;
        } else if (f1 < f2) {
            floatComparisonResult = -1;
        } else {
            floatComparisonResult = 0;
        }
        System.out.println("Float comparison result: " + floatComparisonResult);

        // Test case 2: Double comparison
        double d1 = 100.123;
        double d2 = 100.456;

        int doubleComparisonResult;
        if (d1 > d2) {
            doubleComparisonResult = 1;
        } else if (d1 < d2) {
            doubleComparisonResult = -1;
        } else {
            doubleComparisonResult = 0;
        }
        System.out.println("Double comparison result: " + doubleComparisonResult);

        // Test case 3: Long comparison
        long l1 = 123456789L;
        long l2 = 987654321L;

        int longComparisonResult = (l1 < l2) ? -1 : ((l1 > l2) ? 1 : 0);
        System.out.println("Long comparison result: " + longComparisonResult);

        // Combining results to ensure they are used in further logic
        int combinedResult = floatComparisonResult + doubleComparisonResult + longComparisonResult;
        if (combinedResult > 0) {
            System.out.println("Overall: positive result");
        } else if (combinedResult < 0) {
            System.out.println("Overall: negative result");
        } else {
            System.out.println("Overall: zero result");
        }

        // Complex usage: Looping based on comparisons
        for (int i = 0; i < combinedResult; i++) {
            System.out.println("Loop iteration: " + i);
        }
    }
}
