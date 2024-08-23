public class ArithmeticOperations_mutation_3 {
    public static void main(String[] args) {
        int a = 10;
        int b = 5;
        int sum = a + b;
        int difference = a - b;
        int product = a * b;
        int quotient = calculateQuotient(a, b);

        System.out.println("Sum: " + sum);
        System.out.println("Difference: " + difference);
        System.out.println("Product: " + product);
        System.out.println("Quotient: " + quotient);
    }

    private static int calculateQuotient(int a, int b) {
        return a / b;
    }
}