public class InvokeDynamic_mutation_1 {

    public static void main(String[] args) {
        // Lambda Expression
        Runnable r = () -> System.out.println("Running in a lambda expression!");
        r.run();

        // String Concatenation
        String greeting = "Hello";
        String punctuation = "!";
        System.out.println(greeting + ", World" + punctuation);

        // Method Reference
        InvokeDynamic instance = new InvokeDynamic();
        instance.runMethodReference();
    }

    public void runMethodReference() {
        System.out.println("Method Reference Example");
    }
}
