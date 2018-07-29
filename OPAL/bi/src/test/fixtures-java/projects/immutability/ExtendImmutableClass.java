/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * An immutable class which adds a new field and a new method and also extends an
 * immutable class which has a public final field.
 *
 * @author Andre Pacak
 */
@Immutable("extends an immutable class and only defines fields that are not mutated")
public class ExtendImmutableClass extends ImmutableClass {

    private int y;

    public ExtendImmutableClass() {
        super();
        y = 1;
    }

    public int getY() {
        return y;
    }
}
