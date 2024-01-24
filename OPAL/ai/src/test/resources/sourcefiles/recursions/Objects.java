/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package recursions;

import java.util.List;

/**
 * Class used as fixture for analyses/tests that identify (endless) recursive
 * calls related to objects.
 * 
 * @author Marco Jacobasch
 */
public class Objects {

    public void recursiveEmptyList(List<Object> list) {
        if (list.isEmpty())
            recursiveEmptyList(list);
    }
}
