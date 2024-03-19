/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import OwnAnnotations.Immutable;

/**
 * Some class annotated with a custom Immutable annotation and a trivial mutable field.
 * This should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class CustomAnnotatedWithTrivialMutable {

    public int whatever; // trivial mutable
}
