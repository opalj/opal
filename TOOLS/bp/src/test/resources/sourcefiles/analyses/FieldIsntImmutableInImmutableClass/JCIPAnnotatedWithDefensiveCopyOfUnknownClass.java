/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * Class defensively copying a class with fields of a type unknown to the analysis. As the
 * analysis does not have enough information for an evaluation the unknown class is
 * considered to be mutable and the defensive copy to be too shallow.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithDefensiveCopyOfUnknownClass {

    @SuppressWarnings("unused")
    private MutableClassWithUnknownField foo;

    public JCIPAnnotatedWithDefensiveCopyOfUnknownClass(MutableClassWithUnknownField arg0) {
        foo = new MutableClassWithUnknownField(arg0.foo);
    }
}
