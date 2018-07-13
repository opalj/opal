/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.ConditionallyImmutable;

/**
 * A conditionally immutable class which contains a field that references an object with
 * type ImmutableClass which may, however, hold a reference to an object which is a
 * subtype and which is mutable.
 *
 * @author Andre Pacak
 */
@ConditionallyImmutable("defines a final field which may reference an externally created, mutable object")
public class ReferenceImmutableObjectPassedViaConstructor {

    private final ImmutableClass object;

    public ReferenceImmutableObjectPassedViaConstructor(ImmutableClass object) {
        this.object = object;
    }

    public ImmutableClass getObject() {
        return object;
    }
}
