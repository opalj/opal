/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Mutable;

/**
 * A simple example of a mutable class with a private attribute which is setable via a
 * public method
 *
 * @author Andre Pacak
 */
@Mutable("defines a public setter for a private field")
public class NonFinalPrivateSetter {

    private ImmutableClass object = new ImmutableClass();

    public void setName(ImmutableClass object) {
        this.object = object;
    }

    public ImmutableClass getObject() {
        return object;
    }
}
