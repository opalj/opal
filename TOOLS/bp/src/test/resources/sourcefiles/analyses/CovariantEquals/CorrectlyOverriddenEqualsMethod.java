/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package CovariantEquals;

/**
 * A class with a properly overridden `equals(Object)` and no other. This should not be
 * reported.
 * 
 * @author Daniel Klauer
 */
public class CorrectlyOverriddenEqualsMethod {

    @Override
    public boolean equals(Object other) {
        return true;
    }
}
