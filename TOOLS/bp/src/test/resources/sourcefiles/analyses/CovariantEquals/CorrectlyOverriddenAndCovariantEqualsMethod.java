/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantEquals;

/**
 * A class with a proper `equals(Object)`, and also a covariant one. Since there's a
 * correct `equals()`, we do not report the covariant one.
 * 
 * @author Daniel Klauer
 */
public class CorrectlyOverriddenAndCovariantEqualsMethod {

    @Override
    public boolean equals(Object other) {
        return true;
    }

    public boolean equals(String other) {
        return true;
    }
}
