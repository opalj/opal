/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.Immutable;

/**
 * A immutable class which contains a mutable object and an inner class which has a method
 * that returns the clone of the mutable object
 *
 * @author Andre Pacak
 */
@Immutable("defines a field that stores a mutable object, but that object is never mutated and not made accessible beyond the scope of the inner class")
public class NonStaticInnerClassReturnObjectClone {

    private final MutableClass object;

    public NonStaticInnerClassReturnObjectClone() {
        this.object = new MutableClass();
    }

    public class InnerClass {

        public MutableClass getObject() {
            return object.clone();
        }
    }
}
