/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package BitNops;

/**
 * Various | (OR) no-ops, all of which should be reported by BitNops.
 * 
 * @author Daniel Klauer
 */
public class Or {

    int testZeroLhs(int r) {
        int l = 0;
        return l | r;
    }

    int testZeroRhs(int l) {
        int r = 0;
        return l | r;
    }

    int testZeroBoth() {
        int l = 0;
        int r = 0;
        return l | r;
    }

    int testMinusOneLhs(int r) {
        int l = -1;
        return l | r;
    }

    int testMinusOneRhs(int l) {
        int r = -1;
        return l | r;
    }

    int testMinusOneBoth() {
        int l = -1;
        int r = -1;
        return l | r;
    }
}
