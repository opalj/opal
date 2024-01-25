/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package controlflow;

/**
 * Created to test the computation of control flow graphs.
 * 
 * @author Erich Wittenbeck
 */
public class LoopCode {

    int simpleLoop(int a) {
        int res = 0;
        int i = 0;

        while (i < a) {
            res++;
        }

        return res;
    }

    @SuppressWarnings("unused")
    int nestedLoop(int a, int b) {
        int res = 0;

        for (int i = 1; i < a; i++) {
            for (int j = b; b > 0; b--) {
                res += i * j;
            }
        }

        return 0;
    }

    @SuppressWarnings("unused")
    void endlessLoop(int a, int b, int c) {
        int d = a * b;
        int i = 0;

        while (true) {
            i++;
        }
    }

    int loopWithBranch(int a, int b) {
        int a1 = a;
        int b1 = b;

        if (a1 == 0)
            return b;

        while (b1 != 0) {
            if (a1 > b1)
                a1 = a1 - b1;
            else
                b1 = b1 - a1;
        }

        return a1;
    }
}
