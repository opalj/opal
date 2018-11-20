/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * An immutable class which contains an immutable object and an inner class which has a
 * method that returns the object
 *
 * @author Andre Pacak
 */
@Immutable("the field of the enclosing class, which gets returned by a method of the inner class, is immutable (a downcast is not possible(!))")
public class NonStaticInnerClassReturnImmutableObject {

    private final ImmutableClass object;

    public NonStaticInnerClassReturnImmutableObject() {
        this.object = new ImmutableClass();
    }

    public class InnerClass {

        public ImmutableClass getObject() {
            return object;
        }
    }
}
