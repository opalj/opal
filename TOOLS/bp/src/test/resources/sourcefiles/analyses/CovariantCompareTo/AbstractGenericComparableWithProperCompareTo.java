/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Generic Comparable abstract class with proper compareTo() method: should not be
 * reported.
 * 
 * @author Daniel Klauer
 */
public abstract class AbstractGenericComparableWithProperCompareTo<T> implements
        Comparable<T> {

    // Properly overrides Comparable<T>.compareTo(T)
    @Override
    public int compareTo(T o) {
        return 1;
    }
}
