/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutability;

import immutability.annotations.ConditionallyImmutable;

/**
 * A conditionally immutable class which defines a public field which is an array.
 *
 * @author Andre Pacak
 */
@ConditionallyImmutable("public visible array")
public class FinalPublicArray {

    public final int[] array = new int[10];

}
