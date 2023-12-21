/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * This is a subclass of a proper Comparable class, and it properly overrides the parent's
 * compareTo(). This should not be reported.
 * 
 * @author Daniel Klauer
 */
public class ExtendsProperComparableAndOverrides extends ComparableWithProperCompareTo {

    @Override
    public int compareTo(Object other) {
        return 1;
    }
}
