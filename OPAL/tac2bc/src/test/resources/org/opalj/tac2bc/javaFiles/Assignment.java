public class Assignment {

    public static void main(String[] args) {
        // Test integer assignment
        int a = 10;
        int b = 20;
        int sum = a + b;

        // Test double assignment
        double x = 3.14;
        double y = 2.71;
        double product = x * y;

        // Test string assignment
        String firstName = "John";
        String lastName = "Doe";
        String fullName = firstName + " " + lastName;

        // Test boolean assignment
        boolean isTrue = true;
        boolean isFalse = false;
        boolean andResult = isTrue && isFalse;

        // Test char assignment
        char letter = 'A';
        char nextLetter = (char) (letter + 1);

        // Test long assignment
        long largeNumber = 123456789L;
        long result = largeNumber * 2;

        // Test float assignment
        float pi = 3.14f;
        float halfPi = pi / 2;

        // Test short assignment
        short smallNumber = 100;
        short smallerNumber = (short) (smallNumber - 10);

        // Test byte assignment
        byte byteValue = 10;
        byte incrementedByte = (byte) (byteValue + 1);

        // Test array assignment
        int[] numbers = {1, 2, 3, 4, 5};
        int firstNumber = numbers[0];
        numbers[0] = firstNumber + 10;


        // Print the results
        System.out.println("Sum of integers: " + sum); // 30
        System.out.println("Product of doubles: " + product); // 8.5094
        System.out.println("Full name: " + fullName); // John Doe
        System.out.println("Boolean AND result: " + andResult); // false
        System.out.println("Next letter after 'A': " + nextLetter); // B
        System.out.println("Result of long operation: " + result); // 246913578
        System.out.println("Half of pi (float): " + halfPi); // 1.57
        System.out.println("Smaller short number: " + smallerNumber); // 90
        System.out.println("Incremented byte value: " + incrementedByte); // 11
        System.out.println("First number in array after update: " + numbers[0]); // 11
    }
}