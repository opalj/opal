public class TestingArrays {

    private int[] m = {1, 2, 3, 4, 5};
    private int i = 4;
    private int j = 3;
    public static void main(String[] args) {
        // Isolate the operation that seems to cause issues
        TestingArrays test = new TestingArrays();
        test.doSomething();
    }

    public int doSomething(){
        int k = this.m[this.i] - this.m[this.j];
        System.out.println("k = " + k); // Print the value of k for debugging
        if (k < 0) {
            k += Integer.MAX_VALUE;
            System.out.println("k = " + k); // Print the value of k for debugging
        }
        return k;
    }
}