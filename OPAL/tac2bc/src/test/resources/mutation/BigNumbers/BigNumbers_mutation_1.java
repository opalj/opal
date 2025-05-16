public class BigNumbers_mutation_1 {

    public static void main(String[] args) {
        // Test with int
        int maxValueInt = Integer.MAX_VALUE;
        int largeValueInt = 1_000_000_000;

        int sumInt = maxValueInt + largeValueInt; // Overflow possible
        System.out.println("Int Sum (maxValue + largeValue): " + sumInt);

        if (true) {
            boolean isMaxGreaterThanMinInt = maxValueInt > largeValueInt;
            System.out.println("Is maxValue > largeValue? (int) " + isMaxGreaterThanMinInt);
        }

        // Test with long
        long maxValueLong = Long.MAX_VALUE;
        long largeValueLong = 1_000_000_000_000L;

        long sumLong = maxValueLong + largeValueLong; // Overflow possible
        System.out.println("Long Sum (maxValue + largeValue): " + sumLong);

        boolean isMaxGreaterThanLargeValueLong = maxValueLong > largeValueLong;
        System.out.println("Is maxValue > largeValue? (long) " + isMaxGreaterThanLargeValueLong);

        // Test with double
        double maxValueDouble = Double.MAX_VALUE;
        double largeValueDouble = 1e100;

        double sumDouble = maxValueDouble + largeValueDouble;
        System.out.println("Double Sum (maxValue + largeValue): " + sumDouble);

        boolean isMaxGreaterThanLargeValueDouble = maxValueDouble > largeValueDouble;
        System.out.println("Is maxValue > largeValue? (double) " + isMaxGreaterThanLargeValueDouble);

        // Test with float
        float maxValueFloat = Float.MAX_VALUE;
        float largeValueFloat = 1e30f;

        float sumFloat = maxValueFloat + largeValueFloat;
        System.out.println("Float Sum (maxValue + largeValue): " + sumFloat);

        boolean isMaxGreaterThanLargeValueFloat = maxValueFloat > largeValueFloat;
        System.out.println("Is maxValue > largeValue? (float) " + isMaxGreaterThanLargeValueFloat);
    }
}