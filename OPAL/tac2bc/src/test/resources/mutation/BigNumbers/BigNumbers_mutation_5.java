/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class BigNumbers_mutation_5 {

    public static class MyNumbers {
        public static int maxValueInt = Integer.MAX_VALUE;
        public static long maxValueLong = Long.MAX_VALUE;
        public static double maxValueDouble = Double.MAX_VALUE;
        public static float maxValueFloat = Float.MAX_VALUE;
    }

    public static void main(String[] args) {
        // Test with int
        int largeValueInt = 1_000_000_000;
        int sumInt = MyNumbers.maxValueInt + largeValueInt; // Overflow possible
        System.out.println("Int Sum (maxValue + largeValue): " + sumInt);

        boolean isMaxGreaterThanMinInt = MyNumbers.maxValueInt > largeValueInt;
        System.out.println("Is maxValue > largeValue? (int) " + isMaxGreaterThanMinInt);

        // Test with long
        long largeValueLong = 1_000_000_000_000L;
        long sumLong = MyNumbers.maxValueLong + largeValueLong; // Overflow possible
        System.out.println("Long Sum (maxValue + largeValue): " + sumLong);

        boolean isMaxGreaterThanLargeValueLong = MyNumbers.maxValueLong > largeValueLong;
        System.out.println("Is maxValue > largeValue? (long) " + isMaxGreaterThanLargeValueLong);

        // Test with double
        double largeValueDouble = 1e100;
        double sumDouble = MyNumbers.maxValueDouble + largeValueDouble;
        System.out.println("Double Sum (maxValue + largeValue): " + sumDouble);

        boolean isMaxGreaterThanLargeValueDouble = MyNumbers.maxValueDouble > largeValueDouble;
        System.out.println("Is maxValue > largeValue? (double) " + isMaxGreaterThanLargeValueDouble);

        // Test with float
        float largeValueFloat = 1e30f;
        float sumFloat = MyNumbers.maxValueFloat + largeValueFloat;
        System.out.println("Float Sum (maxValue + largeValue): " + sumFloat);

        boolean isMaxGreaterThanLargeValueFloat = MyNumbers.maxValueFloat > largeValueFloat;
        System.out.println("Is maxValue > largeValue? (float) " + isMaxGreaterThanLargeValueFloat);
    }
}