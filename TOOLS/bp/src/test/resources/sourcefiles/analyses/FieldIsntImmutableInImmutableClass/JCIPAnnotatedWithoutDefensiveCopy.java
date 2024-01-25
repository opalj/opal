/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation and without a defensive copy during
 * output. This should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPAnnotatedWithoutDefensiveCopy {

    private final int[] foo = { 4, 2 };

    // Here a defensive copy should be made.
    public int[] getFoo() {
        return foo;
    }
}
