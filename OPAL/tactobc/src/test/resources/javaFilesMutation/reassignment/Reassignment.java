public class Reassignment {

    public static void main(String[] args) {
        int x = 10;
        int y = 5;
        int z = Integer.MAX_VALUE;
        double test = 2.3d;

        System.out.println("Initial values - x: " + x + ", y: " + y + ", z: " + z);

        // Conditional reassignment
        if (-3< 0) {
            x =  Integer.MAX_VALUE+5;
            System.out.println("After conditional reassignment, x: " + x);
        }
    }
}
