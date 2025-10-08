/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac2bc.single;

public class Constants {

    public static void main(String[] args) {
        // Integer constants
        int intMin1 = -1;
        int int0 = 0;
        int int1 = 1;
        int int2 = 2;
        int int3 = 3;
        int int4 = 4;
        int int5 = 5;
        int int127 = 127;  // Should use BIPUSH
        int int128 = 128;  // Should use SIPUSH
        int int32767 = 32767;  // Maximum short value
        int intBig = 100000;  // Should use LDC

        System.out.println("Integer Constants:");
        System.out.println(intMin1 + ", " + int0 + ", " + int1 + ", " + int2 + ", " + int3 + ", " + int4 + ", " + int5);
        System.out.println(int127 + ", " + int128 + ", " + int32767 + ", " + intBig);

        // Float constants
        float float0 = 0.0f;
        float float1 = 1.0f;
        float float2 = 2.0f;
        float float3Point5 = 3.5f;  // Should use LDC

        System.out.println("Float Constants:");
        System.out.println(float0 + ", " + float1 + ", " + float2 + ", " + float3Point5);

        // Double constants
        double double0 = 0.0;
        double double1 = 1.0;
        double double1Point5 = 1.5;  // Should use LDC

        System.out.println("Double Constants:");
        System.out.println(double0 + ", " + double1 + ", " + double1Point5);

        // Long constants
        long long0 = 0L;
        long long1 = 1L;
        long longBig = 100000L;  // Should use LDC2_W

        System.out.println("Long Constants:");
        System.out.println(long0 + ", " + long1 + ", " + longBig);

        // String constants
        String stringConst = "Hello, World!";
        String stringEmpty = "";
        String stringNull = null;  // Should use ACONST_NULL

        System.out.println("String Constants:");
        System.out.println(stringConst + ", " + stringEmpty);
        System.out.println(stringNull);  // This will print "null"
    }
}
