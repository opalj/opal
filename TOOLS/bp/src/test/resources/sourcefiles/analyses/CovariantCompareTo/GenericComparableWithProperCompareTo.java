/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Generic Comparable class with proper compareTo() method: should not be reported.
 * 
 * @author Daniel Klauer
 */
public class GenericComparableWithProperCompareTo<T> implements Comparable<T> {

    // Properly overrides Comparable<T>.compareTo(T)
    @Override
    public int compareTo(T o) {
        return 1;
    }
}
