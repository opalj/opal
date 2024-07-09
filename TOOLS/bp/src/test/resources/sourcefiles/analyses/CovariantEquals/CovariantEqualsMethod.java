/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantEquals;

/**
 * A class without proper `equals(Object)` but instead a covariant one. This should be
 * reported.
 * 
 * @author Daniel Klauer
 */
public class CovariantEqualsMethod {

    public boolean equals(String other) {
        return true;
    }
}
