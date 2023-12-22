/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Just a very large number of methods that do contain control-flow statements.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithBranches {

    //
    // COMPARISON

    public static boolean nullComp(Object o) {
        if (o == null)
            return true;
        else
            return false;
    }

    public static boolean nonnullComp(Object o) {
        if (o != null)
            return true;
        else
            return false;
    }

    //
    // MULTIPLE COMPARISONS

    /**
     * <pre>
     * public static boolean multipleComp(java.lang.Object a, java.lang.Object b);
     *      0  aload_0 [a]
     *      1  ifnull 17
     *      4  aload_1 [b]
     *      5  ifnull 17
     *      8  aload_0 [a]
     *      9  aload_1 [b]
     *     10  if_acmpne 15
     *     13  iconst_1
     *     14  ireturn
     *     15  iconst_0
     *     16  ireturn
     *     17  iconst_0
     *     18  ireturn
     * </pre>
     */
    public static boolean multipleComp(Object a, Object b) {
        if (a != null && b != null) {
            return a == b;
        } else {
            return false;
        }

    }

    // RELATIONAL OPERATORS
    // Comparing two float/double values puts an int value on the stack which is
    // then compared - by means of an if instruction - with the predefined value
    // 0 and
    // then the result is pushed onto the stack.

    /*
     * FLOAD 0 FLOAD 1 FCMPL IFLE L1
     */
    public static boolean fCompFCMPL(float i, float j) {
        return i > j;
    }

    public static boolean fCompFCMPG(float i, float j) {
        return i < j;
    }

    public static boolean dCompDCMPL(double i, double j) {
        return i > j;
    }

    public static boolean dCompDCMPG(double i, double j) {
        return i < j;
    }

    public static boolean lCompDCMP(long i, long j) {
        return i == j;
    }

    public static boolean dCompDCMPG(int i, int j) {
        return i < j;
    }

    public static int onValue(int value) {
        switch (value) {
        case 1:
            return 100;
        case 203:
            return 455;
        case 29111:
            return 32;
        default:
            return -1;
        }
    }

    public static int onValueDense(int value) {
        switch (value) {
        case 0:
            return 32;
        case 1:
            return 100;
        case 2:
            return 455;
        case 3:
            return 32;
        default:
            return -1;
        }
    }
}
