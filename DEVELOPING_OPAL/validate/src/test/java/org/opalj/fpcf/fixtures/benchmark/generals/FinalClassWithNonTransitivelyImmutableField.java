package org.opalj.fpcf.fixtures.benchmark.generals;

import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

@NonTransitivelyImmutableType("")
@NonTransitivelyImmutableClass("")
public final class FinalClassWithNonTransitivelyImmutableField {

    @NonTransitivelyImmutableField("field has a mutable type and the concrete assigned object can not be determined")
    @NonAssignableField("field is final")
    private final Object finalFieldWithMutableType;
    public FinalClassWithNonTransitivelyImmutableField(Object object){
        this.finalFieldWithMutableType = object;
    }
}
