/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to short values.
 * 
 * @author Marco Jacobasch
 */
public class Shorts {

    public void nonRecursiveCall(short i) {
        return;
    }

    public void recursiveCallFixedNumber(short i) {
        recursiveCallFixedNumber((short) 5);
    }

    public void recursiveCall(short i) {
        recursiveCall(i);
    }

    public void recursiveCallDecrease(short i) {
        recursiveCallDecrease((short) (i - 1));
    }

    public void recursiveCallIncrease(short i) {
        recursiveCallIncrease((short) (i + 1));
    }

    public void boundedRecursiveCall(short i) {
        if (i < 0 || i > 10)
            return;
        boundedRecursiveCall(i);
    }
}
