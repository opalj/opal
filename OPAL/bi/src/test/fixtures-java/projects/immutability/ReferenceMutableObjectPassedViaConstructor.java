/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.ConditionallyImmutable;

/**
 * A mutable class which contains a field that references a mutable object which is passed
 * via constructor.
 * 
 * @author Andre Pacak
 */
@ConditionallyImmutable("references a mutable object that was created externally")
public class ReferenceMutableObjectPassedViaConstructor {

    private final MutableClass object;

    public ReferenceMutableObjectPassedViaConstructor(MutableClass object) {
        this.object = object;
    }

    public MutableClass getObject() {
        return object.clone();
    }
}
