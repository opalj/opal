/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * Subsubclass in the A->B->C class hierarchy.
 * 
 * @author Daniel Klauer
 */
public class C extends B {

    static int C2 = 42;
    static int C4 = 42;
    static int C22 = 42;
    static int C25 = 42;

    static int C_getA26() {
        return A26;
    }

    static int C_getB27() {
        return B27;
    }

    static int C28 = 42;

    static int C_getC28() {
        return C28;
    }

    static int C50 = 42;
}
