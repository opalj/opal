/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class MethodCall_mutation_5 {

    public static void main(String[] args) {
        // Instance method call
        MethodCall_mutation_5 instance = new MethodCall_mutation_5();
        int instanceResult = instance.instanceMethod(5, 3);
        instance.printResult(instanceResult);
    }

    // Instance method
    public int instanceMethod(int a, int b) {
        return a + b;
    }

    // New method to print the result
    public void printResult(int result) {
        System.out.println("Instance method result: " + result);
    }
}
