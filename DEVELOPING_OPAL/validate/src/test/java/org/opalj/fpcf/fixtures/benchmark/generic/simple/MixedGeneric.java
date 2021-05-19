package org.opalj.fpcf.fixtures.benchmark.generic.simple;

import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("Class is not final")
@DependentlyImmutableClass(value="Generic class whichs immutability not depends on all generic type parameters",
        parameter={"C", "B"})
public class MixedGeneric<A, B, C> {

    @DependentImmutableField(value = "final field with generic type", parameter = {"B"})
    @NonAssignableField("field is final")
    private final B b;

    @DependentImmutableField(value = "final field with generic type", parameter = {"C"})
    @NonAssignableField("field is final")
    private final C c;

    public MixedGeneric(A a, B b, C c){
        this.b = b;
        this.c = c;
    }
}
