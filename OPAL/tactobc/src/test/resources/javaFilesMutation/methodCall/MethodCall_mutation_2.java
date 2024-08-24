public class MethodCall_mutation_2 {

    public static void main(String[] args) {
        // Instance method call
        MethodCall_mutation_2 instance = new MethodCall_mutation_2();
        int instanceResult = instance.instanceMethod(5, 3);
        if (true) { // Redundant if statement
            System.out.println("Instance method result: " + instanceResult);
        }
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }
}
