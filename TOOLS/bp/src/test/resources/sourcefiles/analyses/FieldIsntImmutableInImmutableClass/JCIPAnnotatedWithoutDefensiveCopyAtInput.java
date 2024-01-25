/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation and without a defensive copy during
 * input. This should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPAnnotatedWithoutDefensiveCopyAtInput {

    // Final applies only to the array, and not to its elements.
    @SuppressWarnings("unused")
    private final int[] foo;

    // Here a defensive copy should be made, since the source of arg0 still has a
    // reference to the array and can change its elements.
    public JCIPAnnotatedWithoutDefensiveCopyAtInput(int[] args) {
        // This is an error.
        foo = args;
    }
}
