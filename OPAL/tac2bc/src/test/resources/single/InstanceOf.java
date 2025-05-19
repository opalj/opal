/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class InstanceOf {

    public static void main(String[] args) {
        // Test with String
        Object obj = "Hello, World!";
        System.out.println("obj is an instance of String: " + (obj instanceof String)); // true
        System.out.println("obj is an instance of CharSequence: " + (obj instanceof CharSequence)); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of Integer: " + (obj instanceof Integer)); // false

        // Test with Integer
        obj = 42;
        System.out.println("obj is an instance of Integer: " + (obj instanceof Integer)); // true
        System.out.println("obj is an instance of Number: " + (obj instanceof Number)); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of String: " + (obj instanceof String)); // false

        // Test with Array
        obj = new int[]{1, 2, 3};
        System.out.println("obj is an instance of int[]: " + (obj instanceof int[])); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of String[]: " + (obj instanceof String[])); // false
        System.out.println("obj is an instance of Integer[]: " + (obj instanceof Integer[])); // false
    }
}
