public class MethodCall_mutation_1 {

    public static void main(String[] args) {
        // Instance method call
        MethodCall_mutation_1 instance = new MethodCall_mutation_1();
        int instanceResult = instance.instanceMethod(5, 3);
        int tempResult = instanceResult; // Temporary variable
        System.out.println("Instance method result: " + tempResult);
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }
}
