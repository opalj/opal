public class ObjectPropertyAccess {

    // Object attributes
    private int[] m = {10, 20, 30, 40, 50}; // Example array
    private int i; // Index i
    private int j; // Index j
    private int k; // Result of subtraction

    public static void main(String[] args) {
        ObjectPropertyAccess test = new ObjectPropertyAccess();
        test.runTests();
    }

    public void runTests() {
        // Test case 1: Normal subtraction
        this.i = 4; // Refers to m[4] = 50
        this.j = 3; // Refers to m[3] = 40
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 1 (m[4] - m[3]): k = " + this.k); // Expected: 10

        // Test case 2: Subtraction leading to zero
        this.i = 2; // Refers to m[2] = 30
        this.j = 2; // Refers to m[2] = 30
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 2 (m[2] - m[2]): k = " + this.k); // Expected: 0

        // Test case 3: Subtraction leading to a negative result
        this.i = 1; // Refers to m[1] = 20
        this.j = 4; // Refers to m[4] = 50
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 3 (m[1] - m[4]): k = " + this.k); // Expected: -30

        // Test case 4: Subtraction at boundary values (first element)
        this.i = 0; // Refers to m[0] = 10
        this.j = 4; // Refers to m[4] = 50
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 4 (m[0] - m[4]): k = " + this.k); // Expected: -40

        // Test case 5: Subtraction at boundary values (last element)
        this.i = 4; // Refers to m[4] = 50
        this.j = 0; // Refers to m[0] = 10
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 5 (m[4] - m[0]): k = " + this.k); // Expected: 40

        // Test case 6: Subtraction leading to potential overflow/underflow
        this.m = new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE};
        this.i = 0; // Refers to Integer.MAX_VALUE
        this.j = 1; // Refers to Integer.MIN_VALUE
        this.k = this.m[this.i] - this.m[this.j];
        System.out.println("Test 6 (Integer.MAX_VALUE - Integer.MIN_VALUE): k = " + this.k);
        // Expected: Large positive value, may demonstrate overflow behavior
    }
}
