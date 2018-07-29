/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with various complex method signatures.
 *
 * @author Michael Eichberg
 */
public class MethodSignatures {

    static void empty() {
        ;
    }

    static void sTakeInt(int i) {
        ;
    }

    static double sTakeDoubleInt(double d, int i) {
        return d + i;
    }

    static double sTakeDoubleDouble(double d1, double d2) {
        return d1 + d2;
    }

    static void sTakeDoubleIntDouble(double d1, int i, double d2) {
        ;
    }

    static void sTakeDoubleDoubleInt(double d1, double d2, int i) {
        ;
    }

    static void sTakeIntDoubleDouble(int i, double d1, double d2) {
        ;
    }

    void iTakeInt(int i) {
        ;
    }

    double iTakeDoubleInt(double d, int i) {
        iTakeInt(i);
        return d + i;
    }

    void iTakeDoubleDouble(double d1, double d2) {
        ;
    }

    void iTakeDoubleIntDouble(double d1, int i, double d2) {
        ;
    }

    void iTakeDoubleDoubleInt(double d1, double d2, int i) {
        ;
    }

    void iTakeIntDoubleDouble(int i, double d1, double d2) {
        ;
    }
}
