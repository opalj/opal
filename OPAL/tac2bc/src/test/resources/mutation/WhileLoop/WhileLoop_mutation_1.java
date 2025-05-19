/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class WhileLoop_mutation_1 {

    public static void main(String[] args) {
        // Simple while loop
        int counter = 0;
        WhileLoop_mutation_1 myclass = new WhileLoop_mutation_1();
        while (counter < myclass.staticValue) {
            System.out.println("Simple while loop: counter = " + counter);
            counter++;
        }

        // Do-while loop
        int z = 0;
        do {
            System.out.println("Do-while loop: z = " + z);
            z++;
        } while (z < 3);

    }

    public static int staticValue = 5;
}