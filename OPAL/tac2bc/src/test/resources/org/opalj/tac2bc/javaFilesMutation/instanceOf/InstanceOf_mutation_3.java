public class InstanceOf_mutation_3 {

    public static final Object INSTANCE = new Object();

    public static void main(String[] args) {
        // Test with String
        Object obj = INSTANCE;
        System.out.println("obj is an instance of String: " + (obj instanceof String)); // true
        System.out.println("obj is an instance of CharSequence: " + (obj instanceof CharSequence)); // true
        System.out.println("obj is an instance of Object: " + (obj instanceof Object)); // true
        System.out.println("obj is an instance of Integer: " + (obj instanceof Integer)); // false
    }
}