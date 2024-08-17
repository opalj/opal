public class BigNumbers {

    public static void main(String[] args) {
        // Testing with Integer.MAX_VALUE and Integer.MIN_VALUE
        int maxValue = Integer.MAX_VALUE;
        int minValue = Integer.MIN_VALUE;
        int largeValue = 1_000_000_000;
        int largeNegativeValue = -1_000_000_000;

        // Arithmetic operations
        int sum = maxValue + largeValue;         // May cause overflow
        int diff = minValue - largeValue;        // May cause underflow
        int product = maxValue * 2;              // Likely overflow
        int quotient = maxValue / 2;             // Safe operation
        int remainder = maxValue % largeValue;   // Safe operation

        // Comparisons
        boolean isMaxGreaterThanMin = maxValue > minValue;
        boolean isLargeValueGreaterThanMax = largeValue > maxValue;
        boolean isMinLessThanLargeNegativeValue = minValue < largeNegativeValue;
        boolean isEqual = maxValue == Integer.MAX_VALUE;

        // Print results
        System.out.println("Sum (maxValue + largeValue): " + sum);
        System.out.println("Difference (minValue - largeValue): " + diff);
        System.out.println("Product (maxValue * 2): " + product);
        System.out.println("Quotient (maxValue / 2): " + quotient);
        System.out.println("Remainder (maxValue % largeValue): " + remainder);

        System.out.println("Is maxValue > minValue? " + isMaxGreaterThanMin);
        System.out.println("Is largeValue > maxValue? " + isLargeValueGreaterThanMax);
        System.out.println("Is minValue < largeNegativeValue? " + isMinLessThanLargeNegativeValue);
        System.out.println("Is maxValue equal to Integer.MAX_VALUE? " + isEqual);

        // Assignment to test how big numbers are stored and retrieved
        int assignTest = Integer.MAX_VALUE;
        System.out.println("Assigned maxValue: " + assignTest);

        assignTest = Integer.MIN_VALUE;
        System.out.println("Assigned minValue: " + assignTest);

        assignTest = largeValue;
        System.out.println("Assigned largeValue: " + assignTest);

        assignTest = largeNegativeValue;
        System.out.println("Assigned largeNegativeValue: " + assignTest);
    }
}
