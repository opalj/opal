/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Some class annotated with JCIP Immutable annotation and a defensive copy that is not
 * deep enough. This should get reported.
 * 
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
@Immutable
public class JCIPAnnotatedWithTooShallowDefensiveCopy {

    private final NotImmutableWithPublicFields foo = new NotImmutableWithPublicFields(1,
            new int[] { 1, 2, 3 });
    private NotImmutableWithPublicFields[] bar = {
            new NotImmutableWithPublicFields(1, new int[] { 1, 2, 3 }),
            new NotImmutableWithPublicFields(2, new int[] { 1, 2, 3 }) };

    public NotImmutableWithPublicFields getFoo() {
        // No defensive copy for foo.foo, which it should have.
        NotImmutableWithPublicFields v = new NotImmutableWithPublicFields(foo.x, foo.foo);
        return v;
    }

    public NotImmutableWithPublicFields[] getBar() {
        NotImmutableWithPublicFields[] result = new NotImmutableWithPublicFields[bar.length];
        // The elements of the array are mutable. They should be defensively copied
        // instead of being passed right through.
        for (int i = 0; i < bar.length; i++) {
            result[i] = bar[i];
        }
        return result;
    }
}
