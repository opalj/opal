/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.ConditionallyImmutable;

/**
 * A conditionally immutable class which contains a reference to a mutable object and an
 * inner class that has a method which returns the reference.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("defines an inner class that provides access to the mutable object to the outside")
public class NonStaticInnerClassReturnObject {

    private final MutableClass object;

    public NonStaticInnerClassReturnObject() {
        this.object = new MutableClass();
    }

    public class InnerClass {

        public MutableClass getObject() {
            return object;
        }
    }
}
