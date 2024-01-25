/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * This is a subclass of a proper Comparable class, but does not override the parent's
 * compareTo(). We do not generate a report for this case because there are no suspicious
 * covariant compareTo() methods.
 * 
 * @author Daniel Klauer
 */
public class ExtendsProperComparableWithoutOverriding extends
        ComparableWithProperCompareTo {
}
