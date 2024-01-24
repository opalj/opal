/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Self-Comparable class with proper compareTo() method: should not be reported.
 * 
 * @author Daniel Klauer
 */
public class SelfComparableWithProperCompareTo implements
        Comparable<SelfComparableWithProperCompareTo> {

    @Override
    public int compareTo(SelfComparableWithProperCompareTo v) {
        return 1;
    }
}
