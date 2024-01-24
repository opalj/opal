/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package CloneDoesNotCallSuperClone;

/**
 * A class with a `clone()` method that does not have a call to `super.clone()`. This
 * indicates a faulty `clone()` implementation.
 * 
 * @author Daniel Klauer
 */
public class CloneWithoutCallToSuperClone implements Cloneable {

    @Override
    protected Object clone() {
        return new CloneWithoutCallToSuperClone();
    }
}
