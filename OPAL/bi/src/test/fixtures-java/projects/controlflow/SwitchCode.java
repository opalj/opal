/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package controlflow;

/**
 * Created to test the computation of control flow graphs.
 * 
 * @author Erich Wittenbeck
 */
public class SwitchCode {

    public int simpleSwitchWithBreakNoDefault(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res = 123;
            break;
        case 2:
            res = 456;
            break;
        case 3:
            res = 789;
            break;
        }

        return res;
    }

    public int disparateSwitchWithoutBreakWithDefault(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res++;
        case 42:
            res = res * 2;
        case 1337:
            res = res - 34;
        case Integer.MIN_VALUE:
            res = -1;
        default:
            res = 0;
        }

        return res;
    }

    public int withAndWithoutFallthrough(int a) {
        int res = 0;

        switch (a) {
        case 1:
            res = 12;
        case 2:
            res = 34;
        case 3:
            res = 56;
            break;
        case 4:
            res = 78;
            break;
        case 5:
            res = 910;
        default:
            res = 0;
        }

        return res;
    }

    public int degenerateSwitch(int a) {
        switch (a) {
        case 1:
            return 0;
        default:
            return 0;
        }
    }

}
