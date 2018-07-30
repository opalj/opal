/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability.helperclasses;

import immutability.annotations.Immutable;

/**
 * An abstract class which is immutable which contains a non final field.
 *
 * @author Andre Pacak
 */
@Immutable("no field is mutated")
public abstract class AbstractImmutableClass {

    private int x;

    public AbstractImmutableClass(int x) {
        this.x = x;
    }

    public AbstractImmutableClass() {
        this(0);
    }

    public int getX() {
        return this.x;
    }

    public abstract void doSomething();
}
