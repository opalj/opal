/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CovariantCompareTo;

/**
 * An abstract class Comparable to itself, without implementing the proper compareTo()
 * method. This is bad practice because now the derived classes must implement it. It
 * would be better to have the implementation residing in this class, instead of spread
 * out in one or multiple subclasses.
 * 
 * Since there is a covariant compareTo() implemented here, we can assume the author did
 * not want to run into the problem mentioned above.
 * 
 * @author Daniel Klauer
 */
public abstract class AbstractSelfComparableWithCovariantCompareTo implements
        Comparable<AbstractSelfComparableWithCovariantCompareTo> {

    // Does not override compareTo(AbstractSelfCovariant)
    public int compareTo(String v) {
        return 1;
    }
}
