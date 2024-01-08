/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Comparable class with proper compareTo() method: should not be reported.
 * 
 * @author Daniel Klauer
 */
@SuppressWarnings("rawtypes")
public class ComparableWithProperCompareTo implements Comparable {

    // Properly overrides Comparable.compareTo(Object)
    @Override
    public int compareTo(Object o) {
        return 1;
    }
}
