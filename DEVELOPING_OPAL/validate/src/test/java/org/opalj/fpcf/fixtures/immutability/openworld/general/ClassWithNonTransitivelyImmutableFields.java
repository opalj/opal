/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.general;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

@NonTransitivelyImmutableType("Class is final and non transitively immutable")
@NonTransitivelyImmutableClass("Class has only non transitively immutable fields")
public final class ClassWithNonTransitivelyImmutableFields {

    @NonTransitivelyImmutableField("Final field with mutable type assigned in the constructor")
    @NonAssignableField("Declared final Field")
    private final ClassWithMutableFields nonTransitivelyImmutableFieldConstructorAssigned;

    @NonTransitivelyImmutableField("Final field with mutable type eager assigned")
    @NonAssignableField("Declared final Field")
    private final ClassWithMutableFields nonTransitivelyImmutableFieldEagerAssigned = new ClassWithMutableFields();

    @NonTransitivelyImmutableField("Field is assignable and has a non transitively immutable type")
    @NonAssignableField("Declared final Field")
    private final ClassWithNonTransitivelyImmutableFields instance =
            new ClassWithNonTransitivelyImmutableFields(new ClassWithMutableFields());

    public ClassWithNonTransitivelyImmutableFields(ClassWithMutableFields classWithMutableFields){
        this.nonTransitivelyImmutableFieldConstructorAssigned = classWithMutableFields;
    }
}
