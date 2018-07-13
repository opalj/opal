/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Immutable;

/**
 * A simple example of an immutable class with a private final attribute which is of a
 * primitive type.
 *
 * @author Andre Pacak
 */
@Immutable("only defines a final field with a value type")
public class FinalPrivatePrimitiveType {

    private final int x = 1;

    public int getX() {
        return x;
    }
}
