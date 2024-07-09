/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

/**
 * Second mutable class used to form a cyclic dependence.
 * 
 * @author Roberts Kolosovs
 */
public class MutableClassWithCyclicFieldsB {

    // MutableClassWithCyclicFieldsA contains this class as field.
    @SuppressWarnings("unused")
    private MutableClassWithCyclicFieldsA foo = new MutableClassWithCyclicFieldsA();
}
