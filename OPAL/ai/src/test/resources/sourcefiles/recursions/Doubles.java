/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to double values.
 * 
 * @author Marco Jacobasch
 */
public class Doubles {

    public void nonRecursiveCall(double i) {
        return;
    }

    public void recursiveCallFixedNumber(double i) {
        recursiveCallFixedNumber(5);
    }

    public void recursiveCall(double i) {
        recursiveCall(i);
    }

    public void recursiveCallDecrease(double i) {
        recursiveCallDecrease(i - 1);
    }

    public void recursiveCallIncrease(double i) {
        recursiveCallIncrease(i + 1);
    }

    public void boundedRecursiveCall(double i) {
        if (i < 0 || i > 10)
            return;
        boundedRecursiveCall(i);
    }
}
