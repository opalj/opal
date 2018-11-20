/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability.helperclasses;

import immutability.annotations.Immutable;

/**
 * A simple immutable class with a final field with a primitive type.
 * 
 * @author Andre Pacak
 */
@Immutable("only defines a final field with a primitive type (int)")
public class ImmutableClass {

    protected final int x = 0;

    public int getX() {
        return this.x;
    }
}
