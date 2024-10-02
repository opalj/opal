/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation and an direct public setter for a
 * private variable. This should get reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithDirectPublicSetter {

    @SuppressWarnings("unused")
    private int x;

    // This method violates the immutability of the class.
    public void setX(int y) {
        x = y;
    }
}
