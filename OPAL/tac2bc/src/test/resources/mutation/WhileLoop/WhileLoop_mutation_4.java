/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class WhileLoop_mutation_4 {

    public static void main(String[] args) {
        // Simple while loop
        int counter = 0;
        System.out.println("Simple while loop: counter = " + counter);
        while (counter < 5) {
            System.out.println("Simple while loop: counter = " + counter);
            counter++;
        }

        // Do-while loop
        int z = 0;
        z++;
        do {
            System.out.println("Do-while loop: z = " + z);
            z++;
        } while (z < 3);

    }
}