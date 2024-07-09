/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantCompareTo;

/**
 * A generic Comparable abstract class that does not implement the proper compareTo(T)
 * method, but rather a covariant one.
 * 
 * This is bad practice: There should be a proper compareTo() which subclasses can
 * override, rather than an incorrect one which may end up unused in practice: Subclasses
 * are forced to implement the proper compareTo() by the compiler, but then they will
 * override Comparable's compareTo(), instead of that of this abstract class.
 * 
 * @author Daniel Klauer
 */
public abstract class AbstractGenericComparableWithCovariantCompareTo<T> implements
        Comparable<T> {

    // Does not override Comparable<T>.compareTo(T)
    public int compareTo(String o) {
        return 1;
    }
}
