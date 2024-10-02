/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * Self-Comparable abstract class with proper compareTo() method: should not be reported.
 * 
 * @author Daniel Klauer
 */
public abstract class AbstractSelfComparableWithProperCompareTo implements
        Comparable<AbstractSelfComparableWithProperCompareTo> {

    @Override
    public int compareTo(AbstractSelfComparableWithProperCompareTo v) {
        return 1;
    }
}
