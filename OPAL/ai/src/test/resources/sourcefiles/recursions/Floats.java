/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to float values.
 * 
 * @author Marco Jacobasch
 */
public class Floats {

    public void nonRecursiveCall(float i) {
        return;
    }

    public void recursiveCallFixedNumber(float i) {
        recursiveCallFixedNumber(5);
    }

    public void recursiveCall(float i) {
        recursiveCall(i);
    }

    public void recursiveCallDecrease(float i) {
        recursiveCallDecrease(i - 1);
    }

    public void recursiveCallIncrease(float i) {
        recursiveCallIncrease(i + 1);
    }

    public void boundedRecursiveCall(float i) {
        if (i < 0 || i > 10)
            return;
        boundedRecursiveCall(i);
    }
}
