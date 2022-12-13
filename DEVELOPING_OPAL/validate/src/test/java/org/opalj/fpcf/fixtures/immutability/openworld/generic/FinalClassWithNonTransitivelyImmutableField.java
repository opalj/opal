package org.opalj.fpcf.fixtures.immutability.openworld.generic;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

@NonTransitivelyImmutableType("The class is non-transitively immutable and not extensible")
@NonTransitivelyImmutableClass("The class has only a non-transitively immutable field")
public final class FinalClassWithNonTransitivelyImmutableField {

    @NonTransitivelyImmutableField("field has a mutable type and the concrete assigned object cannot be determined")
    @NonAssignableField("field is final")
    private final Object finalFieldWithMutableType;

    public FinalClassWithNonTransitivelyImmutableField(Object object){
        this.finalFieldWithMutableType = object;
    }
}
