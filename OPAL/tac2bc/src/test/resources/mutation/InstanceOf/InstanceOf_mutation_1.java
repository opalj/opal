/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceOf_mutation_1 {

    public static void main(String[] args) {
        // Test with String
        Object obj = "Hello, World!";
        boolean isString = obj instanceof String;
        System.out.println("obj is an instance of String: " + isString); 
        System.out.println("obj is an instance of CharSequence: " + (obj instanceof CharSequence)); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of Integer: " + (obj instanceof Integer)); // false
    }
}