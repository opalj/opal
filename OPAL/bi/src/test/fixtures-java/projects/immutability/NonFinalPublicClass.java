/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.helperclasses.ImmutableClass;
import immutability.annotations.Mutable;

/**
 * A simple example of a mutable class with a public attribute.
 *
 * @author Andre Pacak
 */
@Mutable("defines a public, non-final field")
public class NonFinalPublicClass {

    public ImmutableClass object = new ImmutableClass();

}
