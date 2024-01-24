/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import javax.annotation.concurrent.Immutable;

/**
 * Some class annotated with JSR305 and a trivial mutable field. This should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JSR305AnnotatedWithTrivialMutable {

    public int whatever; // trivial mutable
}
