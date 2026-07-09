/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InvokeDynamic_mutation_4 {

    public static void main(String[] args) {
        // Lambda Expression replaced with Method Reference
        Runnable r = InvokeDynamic_mutation_4::printLambdaMessage;
        r.run();

        // String Concatenation using a method
        printGreeting("Hello", "World");

        // Method Reference
        new InvokeDynamic_mutation_4().methodReferenceExample();
    }

    private static void printLambdaMessage() {
        System.out.println("Running in a lambda!");
    }

    private static void printGreeting(String greeting, String target) {
        System.out.println(greeting + ", " + target + "!");
    }

    public void methodReferenceExample() {
        System.out.println("Method Reference Example");
    }
}
