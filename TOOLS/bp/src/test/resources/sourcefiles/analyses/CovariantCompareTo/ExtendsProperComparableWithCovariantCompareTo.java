/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CovariantCompareTo;

/**
 * This is a subclass of a proper Comparable class, but does not override the parent
 * class' compareTo(). Instead there is a suspicious covariant compareTo() method. This
 * case should be reported, because we can assume that the programmer intended to override
 * the parent's compareTo() but just used the wrong parameter types accidentally.
 * 
 * @author Daniel Klauer
 */
public class ExtendsProperComparableWithCovariantCompareTo extends
        ComparableWithProperCompareTo {

    // Does not implement Comparable.compareTo(Object),
    // but this could be intentional, so no report.
    int compareTo(String other) {
        return 1;
    }
}
