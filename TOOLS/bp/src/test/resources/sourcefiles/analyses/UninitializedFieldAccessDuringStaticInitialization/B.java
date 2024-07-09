/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * Subclass in the A->B->C class hierarchy.
 * 
 * @author Daniel Klauer
 */
public class B extends A {

    static int B1 = 42;
    static int B3 = 42;
    static int B21 = 42;

    static int B_getA23() {
        return A23;
    }

    static int B24 = 42;

    static int B_getB24() {
        return B24;
    }

    static int B_getC25() {
        return C.C25;
    }

    static int B27 = 42;
    static int B31 = 42;

    static int B_getA32() {
        return A32;
    }

    static int B33 = 42;

    static int B_getB33() {
        return B33;
    }

    static int B_getA34_viaA() {
        return A_getA34();
    }

    static int B35 = 42;

    static int B_getB35_viaA() {
        return A_getB35();
    }

    static int B_getA36() {
        return A36;
    }

    static int B_getA36_viaB() {
        return B_getA36();
    }

    static int B37 = 42;

    static int B_getB37() {
        return B37;
    }

    static int B_getB37_viaB() {
        return B_getB37();
    }

    static int samename1 = 42;
    static int samename2 = 42;

    static int B50 = 42;
    static int B60 = 42;
    static int B70 = 42;
    static int B71 = 42;
    static int B72 = 42;

    static void B_initB72() {
        B72 = 42;
    }
}
