public class Constants {

    public static void main(String[] args) {
        // Integer constants
        int intMin1 = -1;
        int int0 = 0;
        int int1 = 1;
        int int127 = 127;  // BIPUSH
        int intBig = 100000;  // LDC

        System.out.println("Integer Constants:");
        System.out.println(intMin1 + ", " + int0 + ", " + int1 + ", " + int127 + ", " + intBig);

        // Float constants
        float float0 = 0.0f;
        float float1 = 1.0f;

        System.out.println("Float Constants:");
        System.out.println(float0 + ", " + float1);

        // Double constants
        double double0 = 0.0;
        double double1 = 1.0;

        System.out.println("Double Constants:");
        System.out.println(double0 + ", " + double1);

        // Long constants
        long long0 = 0L;
        long long1 = 1L;

        System.out.println("Long Constants:");
        System.out.println(long0 + ", " + long1);

        // String constants
        String stringConst = "Hello!";
        String stringEmpty = "";

        System.out.println("String Constants:");
        System.out.println(stringConst + ", " + stringEmpty);
    }
}
