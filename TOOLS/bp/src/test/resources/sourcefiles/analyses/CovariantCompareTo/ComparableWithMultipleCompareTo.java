/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Comparable with multiple compareTo() methods, one of which is the correct one. Since
 * the correct compareTo() is not missing, we should not generate a report.
 * 
 * @author Daniel Klauer
 */
@SuppressWarnings("rawtypes")
public class ComparableWithMultipleCompareTo implements Comparable {

    // Properly overrides Comparable.compareTo(Object)
    @Override
    public int compareTo(Object o) {
        return 1;
    }

    // This 2nd compareTo() should not cause a report
    public int compareTo(String o) {
        return 1;
    }
}
