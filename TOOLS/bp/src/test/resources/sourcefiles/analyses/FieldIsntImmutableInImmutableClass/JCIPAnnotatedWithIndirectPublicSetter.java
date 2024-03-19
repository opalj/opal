/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation and an indirect public setter. This
 * should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPAnnotatedWithIndirectPublicSetter {

    @SuppressWarnings("unused")
    private int x;

    private void setX(int y) {
        x = y;
    }

    // Public method that indirectly sets x.
    public void indirect(int a, int b) {
        // With this x really isn't that immutable any more.
        setX(a + b);
    }
}
