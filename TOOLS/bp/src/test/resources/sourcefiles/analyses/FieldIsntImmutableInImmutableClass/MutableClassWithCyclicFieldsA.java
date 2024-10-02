/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package FieldIsntImmutableInImmutableClass;

/**
 * First mutable class used to form a cyclic dependence.
 * 
 * @author Roberts Kolosovs
 */
public class MutableClassWithCyclicFieldsA {

    // MutableClassWithCyclicFieldsB contains this class as field.
    @SuppressWarnings("unused")
    private MutableClassWithCyclicFieldsB bar = new MutableClassWithCyclicFieldsB();
}
