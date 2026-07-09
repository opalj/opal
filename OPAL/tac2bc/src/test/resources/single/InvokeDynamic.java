/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InvokeDynamic {

    public static void main(String[] args) {
        // Test 1: Lambda Expression
        Runnable r = () -> System.out.println("Running in a lambda!");
        r.run();

        // Test 2: String Concatenation with invokedynamic
        String str1 = "Hello";
        String str2 = "World";
        String result = str1 + ", " + str2 + "!";
        System.out.println(result);

        // Test 3: Method Reference
        InvokeDynamic test = new InvokeDynamic();
        Runnable r2 = test::methodReferenceExample;
        r2.run();
    }

    public void methodReferenceExample() {
        System.out.println("Method Reference Example");
    }
}
