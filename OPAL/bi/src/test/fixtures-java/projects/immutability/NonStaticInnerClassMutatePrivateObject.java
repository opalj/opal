/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.ConditionallyImmutable;

/**
 * A mutable class which contains a private final mutable object and an inner class that
 * provides a method that enables mutation of the object.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("the inner class mutates a field of the enclosing class")
public class NonStaticInnerClassMutatePrivateObject {

    private final MutableClass object;

    public NonStaticInnerClassMutatePrivateObject() {
        this.object = new MutableClass();
    }

    public class InnerClass {

        public void doSomething() {
            object.x = 42;
        }
    }
}
