/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * Defines a public, non-final field.
 * 
 * @author Andre Pacak
 */
@Mutable("defines a public, non-final field")
public class VolatileFieldClass {

    public volatile int x;

    public int getX() {
        return x;
    }
}
