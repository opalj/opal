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

        // Loop with reassignment
        for (int i = 0; i < 3; i++) {
            z = z + (x * i) - y;
            System.out.println("After loop iteration " + i + ", z: " + z);
        }

        // Nested reassignments
        if (z > 10) {
            x = x * 2;
            y = y + z;
            z = x - y;
            System.out.println("After nested reassignment, x: " + x + ", y: " + y + ", z: " + z);
        }

        // Reassign using previous values
        z = (x * y) + z;
        System.out.println("Final value of z after using previous values: " + z);

        // Complex reassignments with multiple operations
        x = x + (y - z) * 2;
        y = y * 3 + z - x;
        z = x - y + z;
        System.out.println("Complex final values - x: " + x + ", y: " + y + ", z: " + z);
    }
}
