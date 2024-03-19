/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package InefficientToArray;

import java.util.ArrayList;

/**
 * Fixed version of InefficientToArray, should not be reported by the analysis.
 * 
 * @author Daniel Klauer
 */
public class EfficientToArray {

    void test() {
        ArrayList<Integer> list = new ArrayList<Integer>();

        // Passing "new Integer[...]" to get the correct result type
        // (Integer[]),
        // because toArray() alone would return Object[].
        //
        // Doing new Integer[list.size()] instead of new Integer[0] to allow
        // toArray() to
        // re-use
        // that array to return the result, instead of having to allocate a new
        // one.
        @SuppressWarnings("unused")
        Integer[] array = list.toArray(new Integer[list.size()]);
    }
}
