/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Self-Comparable with multiple compareTo() methods, one of which is the correct one.
 * Since the correct compareTo() is not missing, we should not generate a report.
 * 
 * @author Daniel Klauer
 */
public class SelfComparableWithMultipleCompareTo implements
        Comparable<SelfComparableWithMultipleCompareTo> {

    @Override
    public int compareTo(SelfComparableWithMultipleCompareTo v) {
        return 1;
    }

    // This 2nd compareTo() should not cause a report
    public int compareTo(String o) {
        return 1;
    }
}
