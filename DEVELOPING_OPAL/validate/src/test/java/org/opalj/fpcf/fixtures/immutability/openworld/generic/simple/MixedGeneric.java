/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentlyImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;
/*
 * This class represents a generic class whose immutability does not depend on all generic type parameters
 */
@MutableType("Class is not final")
@DependentlyImmutableClass(value="Generic class whose immutability does not depend on all generic type parameters",
        parameter={"C", "B"})
public class MixedGeneric<A, B, C> {

    @DependentlyImmutableField(value = "final field with generic type", parameter = {"B"})
    @NonAssignableField("field is final")
    private final B b;

    @DependentlyImmutableField(value = "final field with generic type", parameter = {"C"})
    @NonAssignableField("field is final")
    private final C c;

    public MixedGeneric(A a, B b, C c){
        this.b = b;
        this.c = c;
    }
}
