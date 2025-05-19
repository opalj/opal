/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Compare_mutation_3 {

    public static void main(String[] args) {
        // Float comparison
        float f1 = 10.5f;
        float f2 = 20.5f;
        int floatComparisonResult = Float.compare(f1, f2);
        int floatResult = floatComparisonResult; // Store the result in a temporary variable
        System.out.println("Float comparison result: " + floatResult);

        // Double comparison
        double d1 = 100.123;
        double d2 = 100.456;
        int doubleComparisonResult = Double.compare(d1, d2);
        int doubleResult = doubleComparisonResult; // Store the result in a temporary variable
        System.out.println("Double comparison result: " + doubleResult);

        // Long comparison
        long l1 = 123456789L;
        long l2 = 987654321L;
        int longComparisonResult = Long.compare(l1, l2);
        int longResult = longComparisonResult; // Store the result in a temporary variable
        System.out.println("Long comparison result: " + longResult);

        // Combining results
        int combinedResult = floatResult + doubleResult + longResult;
        System.out.println("Combined result: " + combinedResult);
    }
}