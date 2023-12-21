/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package InefficientToArray;

import java.util.ArrayList;

/**
 * A class with a method using toArray() in an inefficient way. Should be reported by the
 * analysis.
 * 
 * @author Daniel Klauer
 */
public class InefficientToArray {

    void test() {
        ArrayList<Integer> list = new ArrayList<Integer>();

        // Passing "new Integer[0]" to get the correct result type (Integer[]),
        // because toArray() alone would return Object[].
        @SuppressWarnings("unused")
        Integer[] array = list.toArray(new Integer[0]);
    }
}
