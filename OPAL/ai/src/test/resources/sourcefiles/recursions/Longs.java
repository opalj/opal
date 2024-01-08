/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to long values.
 * 
 * @author Marco Jacobasch
 */
public class Longs {

    public void nonRecursiveCall(long i) {
        return;
    }

    public void recursiveCallFixedNumber(long i) {
        recursiveCallFixedNumber(5);
    }

    public void recursiveCall(long i) {
        recursiveCall(i);
    }

    public void recursiveCallDecrease(long i) {
        recursiveCallDecrease(i - 1);
    }

    public void recursiveCallIncrease(long i) {
        recursiveCallIncrease(i + 1);
    }

    public void boundedRecursiveCall(long i) {
        if (i < 0 || i > 10)
            return;
        boundedRecursiveCall(i);
    }
}
