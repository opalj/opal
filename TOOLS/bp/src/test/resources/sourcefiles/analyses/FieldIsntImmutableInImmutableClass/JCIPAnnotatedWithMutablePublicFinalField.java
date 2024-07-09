/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some Class annotated with Immutable and a mutable public final field. This should get
 * reported.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithMutablePublicFinalField {

    // Field is a reference to a mutable type. final isn't enough to ensure immutability.
    public final NotImmutableWithPublicFields mutable = new NotImmutableWithPublicFields();
}
