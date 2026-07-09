/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InvokeDynamic_mutation_2 {

    public static void main(String[] args) {
        // Lambda Expression
        Runnable r = () -> printLambdaMessage();
        r.run();

        // String Concatenation
        String message = buildMessage("Hello", "World");
        System.out.println(message);

        // Method Reference
        new InvokeDynamic_mutation_2().methodReference();
    }

    private static void printLambdaMessage() {
        System.out.println("Running in a lambda!");
    }

    private static String buildMessage(String part1, String part2) {
        return part1 + ", " + part2 + "!";
    }

    public void methodReference() {
        System.out.println("Method Reference Example");
    }
}
