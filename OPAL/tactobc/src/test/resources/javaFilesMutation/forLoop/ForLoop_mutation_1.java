public class ForLoop_mutation_1 {

    public static void main(String[] args) {
        // Simple for loop with int
        int sum = 0;
        int temp = 0;
        for (int i = 0; i < 5; i++) {
            temp = i;
            sum += temp;
        }
        System.out.println("Sum of first 5 numbers: " + sum);

        // For loop with an array of integers
        int[] numbers = {1, 2, 3};
        int arraySum = 0;
        for (int i = 0; i < numbers.length; i++) {
            arraySum += numbers[i];
        }
        System.out.println("Sum of array elements: " + arraySum);

        // For loop with a double type
        double product = 1.0;
        for (int i = 1; i <= 3; i++) {
            product *= i * 1.5;
        }
        System.out.println("Product of first 3 numbers multiplied by 1.5: " + product);
    }
}