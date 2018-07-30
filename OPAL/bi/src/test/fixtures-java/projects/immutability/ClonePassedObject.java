/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.Immutable;

/**
 * An immutable class which gets a reference passed via the constructor but creates a
 * clone and returns a clone in the public method.
 * 
 * @author Andre Pacak
 */
@Immutable("the mutable object referred to is not mutated by the class and is only referred to by the class (no aliases exist)")
public class ClonePassedObject {

    private final MutableClass reference;

    public ClonePassedObject(MutableClass reference) {
        this.reference = reference.clone();
    }

    public MutableClass getReference() {
        return reference.clone();
    }
}
