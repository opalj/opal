/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Immutable;

/**
 * A simple example of an immutable class with a public final attribute which is of a
 * reference type which is immutable itself.
 *
 * @author Andre Pacak
 */
@Immutable("the object referred to by the final, public field is immutable")
public class FinalPublicImmutableObject {

    public final ImmutableClass object = new ImmutableClass();
}
