/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to String values.
 * 
 * @author Marco Jacobasch
 */
public class Strings {

    public void recursiveIfEmptyString(String str) {
        if (str.isEmpty())
            recursiveIfEmptyString(str);
    }

}
