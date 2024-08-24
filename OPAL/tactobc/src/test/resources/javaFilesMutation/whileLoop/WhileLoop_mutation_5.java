public class WhileLoop_mutation_5 {

    public static void main(String[] args) {
        // Simple while loop
        int[] counterArray = {0};
        while (counterArray[0] < 5) {
            System.out.println("Simple while loop: counter = " + counterArray[0]);
            counterArray[0]++;
        }

        // Do-while loop
        int z = 0;
        int[] zArray = {z};
        do {
            System.out.println("Do-while loop: z = " + zArray[0]);
            zArray[0]++;
        } while (zArray[0] < 3);

    }
}