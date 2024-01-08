/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to Integer values.
 * 
 * @author Marco Jacobasch
 */
public class Integers {

    // nothing
    public void nonRecursiveCall(int i) {
        return;
    }

    // finite with 2 branches
    public void nonRecursiveCallA(int i) {
        if (i == 0)
            return;
        return;
    }

    // finite, one additional self invoke if argument is not 0
    public void nonRecursiveCallB(int i) {
        if (i == 0)
            return;
        else {
            nonRecursiveCallB(0);
        }
    }

    // finite recursion, abort by NPE
    @SuppressWarnings("null")
    public void nonRecursiveCallThrows() {
        String str = null;
        try {
            str.length();
            nonRecursiveCallThrows();
        } catch (Exception e) {
        }
    }

    // infinite recursion, no arguments
    public void recursiveCallNoArguments() {
        recursiveCallNoArguments();
    }

    // infinite with a fixed value of 5.
    public void recursiveCallFixedNumber(int i) {
        recursiveCallFixedNumber(5);
    }

    // infinite recursion, non changing value
    public void recursiveCall(int i) {
        recursiveCall(i);
    }

    // infinite recursion, decreasing value
    public void recursiveCallDecrease(int i) {
        recursiveCallDecrease(i - 1);
    }

    // infinite recursion, increasing value
    public void recursiveCallIncrease(int i) {
        recursiveCallIncrease(i + 1);
    }

    // finite recursion, bounds 0 to 10
    public void nonRecursiveboundedCall(int i) {
        if ((i < 0) || (i > 10))
            return;
        nonRecursiveboundedCall(i - 1);
    }

    // infinite if any even number, break if odd and if > 10
    public void randomRecursion(int i) {
        if (i > 0 && i < 10) {
            randomRecursion(i);
        } else if (i > 10) {
            return;
        } else {
            randomRecursion(i + 2);
        }
    }

    // infinite recursion if value < 0, else finite
    public void recursiveCallT(int i) {
        if (i < 0) {
            recursiveCallT(-1);
        } else if (0 == i) {
            recursiveCallT(1);
        } else if (1 == i) {
            recursiveCallT(2);
        } else if (2 == i) {
            recursiveCallT(3);
        } else if (3 == i) {
            recursiveCallT(4);
        } else if (4 == i) {
            recursiveCallT(5);
        } else {
            return;
        }
    }

    // infinite recursion if value not 0
    public void recursiveCallConditional(int i) {
        if (0 == i) {
            return;
        } else {
            recursiveCallConditional(i);
        }
    }

    // infinite mutual recursion. Variant A
    public void recursiveCallCycleA(int i) {
        recursiveCallCycleB(i);
    }

    // infinite mutual recursion. Variant B
    public void recursiveCallCycleB(int i) {
        recursiveCallCycleA(i);
    }

    // static infinite recursion with fixed value
    static void staticRecursiveFixedValue(int i) {
        staticRecursiveFixedValue(10);
    }

    public void lateRecursiveCall(int i) {
        if (i < 10) {

        }
    }

    // field providing a side effect for recursiveCallAndSideEffect
    public boolean abort = false;

    // using field 'abort' to break out of an infinite recursion
    public void recursiveCallAndSideEffect(int i) {
        if (abort)
            return;
        recursiveCallAndSideEffect(i);
    }
}
