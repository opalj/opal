/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package controlflow;

/**
 * Created to test the computation of control flow graphs.
 *  
 * @author Erich Wittenbeck
 */
public class BoringCode {

    int singleBlock() {
        int a = 3;
        int b = 4;

        a = a + b;
        b = a - b;
        a = a - b;

        return a;
    }

    @SuppressWarnings("unused")
    boolean conditionalTwoReturns(int a) {
        int b = a;
        int c = a % 2;

        if (c == 0)
            return false;
        if (c == 1)
            return true;

        return false;
    }

    boolean conditionalOneReturn(int a, boolean b) {
        int c = a + 123;
        boolean d;

        if (b)
            d = (c % 2) == 0;
        else
            d = c < 500;

        return d;
    }

}
