/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Abstract self-Comparable with multiple compareTo() methods, one of which is the correct
 * one. Since the correct compareTo() is not missing, we should not generate a report.
 * 
 * @author Daniel Klauer
 */
public abstract class AbstractSelfComparableWithMultipleCompareTo implements
        Comparable<AbstractSelfComparableWithMultipleCompareTo> {

    @Override
    public int compareTo(AbstractSelfComparableWithMultipleCompareTo v) {
        return 1;
    }

    // This 2nd compareTo() should not cause a report
    public int compareTo(String o) {
        return 1;
    }
}
