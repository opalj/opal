/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.Immutable;

/**
 * A simple example of an immutable class with a public final field which is of a
 * primitive type.
 *
 * @author Andre Pacak
 */
@Immutable("defines a final field with primitive type")
public class FinalPublicPrimitiveType {

    public final int x = 1;
}
