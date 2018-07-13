/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability.helperclasses;

import immutability.annotations.Mutable;

/**
 * A simple mutable class with a public int field.
 * 
 * @author Andre Pacak
 */
@Mutable("defines a public non-final primitive field")
public class MutableClass implements Cloneable {

    public int x = 0;

    public MutableClass clone() {
        return new MutableClass();
    }
}
