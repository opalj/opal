/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * A mutable class with a non-pure method for a private field. A public method calls the
 * private method.
 * 
 * @author Andre Pacak
 */
@Mutable("defines a public method that potentially mutates the state of the object")
public class IndirectSetterForPrivateField {

    private int x = 0;
    public final int y;

    public IndirectSetterForPrivateField(int y) {
        this.y = y;
    }

    public int getX() {
        return this.x;
    }

    private void changeX() {
        this.x = 1;
    }

    public void doSomething() {
        changeX();
    }
}
