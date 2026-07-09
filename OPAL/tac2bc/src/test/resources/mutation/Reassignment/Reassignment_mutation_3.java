/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Reassignment_mutation_3 {

    public static void main(String[] args) {
        int x = 10;
        int y = 5;
        int z = Integer.MAX_VALUE;
        double test = 2.3d;

        System.out.println("Initial values - x: " + x + ", y: " + y + ", z: " + z);

        // Conditional reassignment
        x = performCalculation(x);
        System.out.println("After conditional reassignment, x: " + x);
    }

    public static int performCalculation(int x) {
        if (-3 < 0) {
            return Integer.MAX_VALUE + 5;
        }
        return x;
    }
}