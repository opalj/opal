/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * Methods which perform various math operations.
 *
 * @author Michael Eichberg
 */
public class ConditionalMath {

    static int m1(int p) {
        int i;

        if (p == 1)
            i = 7;
        else
            i = 5;

        int j;
        if (i <= 5)
            j = i;
        else
            j = Integer.MAX_VALUE;

        return j + j;
    }

    static int m2(int p1, int p2) {
        if (p1 > 0)
            return m2(p1 - 1, p2) + 1;
        else if (p2 == 0)
            return 1;
        else
            return m2(p2, p2 - 1) + 1;
    }

    static int m3(int p) {
        try {
            return 100 / p;
        } catch (ArithmeticException ae) {
            return Integer.MIN_VALUE + p;
        }
    }

    static int max5(int l) {

        int i;
        if (l < 5) {
            i = l;
        } else {
            i = 5;
        }
        return i;
    }

    static int aliases(int l) {
        int p = 0;
        int i;
        if (l < 5) {
            i = l;
        } else {
            p = l;
            if (p < 5) {
                // this line should never be reached... p is an alias of l and l is larger or
                // equal to 5
                throw new UnknownError();
            } else {
                i = -5;
            }
        }
        return i;
    }

    public static void main(String[] args) {
        System.out.println(m1(100));
        System.out.println(m2(1, 1));
        System.out.println(m2(2, 2));
        System.out.println(m2(3, 3));
        System.out.println(m3(100));
    }

}
