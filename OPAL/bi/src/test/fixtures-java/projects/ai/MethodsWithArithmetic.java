/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Methods that perform arithmetic operations.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithArithmetic {

    public static short simpleMathUsingShortValues(short value) {
        short s = (short) Integer.MAX_VALUE;
        short s2 = (short) (value - s);
        return (short) (s2 * 23);
    }

    public static int fak() {
        int MAX = 5;
        int r = 1;
        for (int i = 1; i < MAX; i++) {
            r = r * i;
        }
        return r;
    }

    public static int divIt(int denominator) {
        return 3 / denominator;
    }

    public static int divItSafe(int denominator) {
        if (denominator == 0)
            return 0;
        else
            return 3 / denominator;
    }

    public static short returnShortValue(short value) {
        return value;
    }

}
