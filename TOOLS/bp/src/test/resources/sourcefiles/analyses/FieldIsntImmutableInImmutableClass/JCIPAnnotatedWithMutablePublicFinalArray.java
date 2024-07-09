/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some Class annotated with Immutable and a public final array. This should get reported,
 * as Arrays are always mutable.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPAnnotatedWithMutablePublicFinalArray {

    // The ELements of the Array can still be changed, even if the Array is final.
    public final int[] foo = { 1, 2, 3 };
}
