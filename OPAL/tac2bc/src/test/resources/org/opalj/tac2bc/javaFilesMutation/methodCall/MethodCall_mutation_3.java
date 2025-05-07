public class MethodCall_mutation_3 {

    public static void main(String[] args) {
        // Instance method call inlined
        MethodCall_mutation_3 instance = new MethodCall_mutation_3();
        System.out.println("Instance method result: " + instance.instanceMethod(5, 3));
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }
}
