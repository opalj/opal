/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * A simple example of an immutable class with a private final field which is of a
 * reference type that is immutable itself.
 *
 * @author Andre Pacak
 */
@Immutable("defines a final field whose value is immutable")
public class PrivateImmutableObject {

    private ImmutableClass object;

    public PrivateImmutableObject() {
        this.object = new ImmutableClass();
    }

    public ImmutableClass getObject() {
        return object;
    }
}
