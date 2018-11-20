/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.MutableClass;
import immutability.annotations.ConditionallyImmutable;;

/**
 * A conditionally immutable class with a final field which references a mutable object.
 *
 * @author Andre Pacak
 */
@ConditionallyImmutable("the object referred to by the public, final field is mutable")
public class FinalPublicMutableObject {

    public final MutableClass object = new MutableClass();
    
}
