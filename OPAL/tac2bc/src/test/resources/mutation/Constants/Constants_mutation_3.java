/* BSD 2-Clause License - see OPAL/LICENSE for details. */
public class Constants_mutation_3 {

    public static void main(String[] args) {
        // Integer constants
        int[] intArray = new int[]{-1, 0, 1, 127, 100000};
        int intMin1 = intArray[0];
        int int0 = intArray[1];
        int int1 = intArray[2];
        int int127 = intArray[3];  // BIPUSH
        int intBig = intArray[4];  // LDC

        System.out.println("Integer Constants:");
        System.out.println(intMin1 + ", " + int0 + ", " + int1 + ", " + int127 + ", " + intBig);

        // Float constants
        float[] floatArray = new float[]{0.0f, 1.0f};
        float float0 = floatArray[0];
        float float1 = floatArray[1];

        System.out.println("Float Constants:");
        System.out.println(float0 + ", " + float1);

        // Double constants
        double[] doubleArray = new double[]{0.0, 1.0};
        double double0 = doubleArray[0];
        double double1 = doubleArray[1];

        System.out.println("Double Constants:");
        System.out.println(double0 + ", " + double1);

        // Long constants
        long[] longArray = new long[]{0L, 1L};
        long long0 = longArray[0];
        long long1 = longArray[1];

        System.out.println("Long Constants:");
        System.out.println(long0 + ", " + long1);

        // String constants
        String[] stringArray = new String[]{"Hello!", ""};
        String stringConst = stringArray[0];
        String stringEmpty = stringArray[1];

        System.out.println("String Constants:");
        System.out.println(stringConst + ", " + stringEmpty);
    }
}