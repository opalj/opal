/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Mutable;

/**
 * A at first glance an immutable class. But it contains an inner class which adds a
 * public method that mutates the private field.
 *
 * @author Andre Pacak
 */
@Mutable("the inner class implements a setter for a private field of the enclosing class")
public class NonStaticInnerClassMutatePrivatePrimitive {

    private int x;

    public NonStaticInnerClassMutatePrivatePrimitive(int x) {
        this.x = x;
    }

    public int getX() {
        return x;
    }

    public class InnerClass {

        public void setX(int newX) {
            x = newX;
        }
    }
}
