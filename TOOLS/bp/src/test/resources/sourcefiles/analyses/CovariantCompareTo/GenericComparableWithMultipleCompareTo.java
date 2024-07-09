/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Generic Comparable class with multiple compareTo() methods, one of which is the correct
 * one. Since the correct compareTo() is not missing, we should not generate a report.
 * 
 * @author Daniel Klauer
 */
public class GenericComparableWithMultipleCompareTo<T> implements Comparable<T> {

    // Properly overrides Comparable<T>.compareTo(T)
    @Override
    public int compareTo(T o) {
        return 1;
    }

    // This 2nd compareTo() should not cause a report
    public int compareTo(String o) {
        return 1;
    }
}
