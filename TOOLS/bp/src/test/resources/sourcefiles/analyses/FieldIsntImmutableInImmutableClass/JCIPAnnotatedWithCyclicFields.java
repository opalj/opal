/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

import net.jcip.annotations.Immutable;

/**
 * This immutable class has classes with cyclic dependences as fields. This can cause the
 * analysis to execute an infinite recursion and crash with a stack overflow error.
 * 
 * @author Roberts Kolosovs
 */
@Immutable
public class JCIPAnnotatedWithCyclicFields {

    // The field classes contain each other as fields.
    // This cycle can crash the analysis if not handled properly.
    @SuppressWarnings("unused")
    private MutableClassWithCyclicFieldsA foo = new MutableClassWithCyclicFieldsA();
    @SuppressWarnings("unused")
    private MutableClassWithCyclicFieldsB bar = new MutableClassWithCyclicFieldsB();
}
