public class ObjectPropertyAccess_mutation_3 {

    // Object attributes
    private int[] m = {10, 20, 30, 40, 50}; // Example array
    private int i; // Index i
    private int j; // Index j
    private int k; // Result of subtraction

    public static void main(String[] args) {
        ObjectPropertyAccess_mutation_3 test = new ObjectPropertyAccess_mutation_3();
        test.runTests();
    }

    public void runTests() {
        // Test case 1: Normal subtraction
        this.i = 4; // Refers to m[4] = 50
        this.j = 3; // Refers to m[3] = 40
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 1 (m[4] - m[3]): k = " + (this.m[this.i] - this.m[this.j])); // Inlined subtraction
    }
}
