/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceOf_mutation_4 {

    public static String mystaticstring = "Hello, World!";

    public static void main(String[] args) {
        // Test with String
        Object obj = mystaticstring;
        System.out.println("obj is an instance of String: " + (obj instanceof String)); // true
        System.out.println("obj is an instance of CharSequence: " + (obj instanceof CharSequence)); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of Integer: " + (obj instanceof Integer)); // false
    }
}