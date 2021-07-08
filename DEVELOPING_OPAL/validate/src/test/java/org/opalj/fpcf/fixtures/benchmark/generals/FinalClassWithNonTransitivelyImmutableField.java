package org.opalj.fpcf.fixtures.benchmark.generals;

import org.opalj.fpcf.fixtures.benchmark.commons.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.NonAssignableField;
import org.opalj.fpcf.properties.immutability.types.NonTransitivelyImmutableType;

//@Immutable
@NonTransitivelyImmutableType("")
@NonTransitivelyImmutableClass("")
public final class FinalClassWithNonTransitivelyImmutableField {

    //@Immutable
    @NonTransitivelyImmutableField("field has a mutable type and the concrete assigned object can not be determined")
    @NonAssignableField("field is final")
    private final CustomObject finalFieldWithMutableType;
    public FinalClassWithNonTransitivelyImmutableField(CustomObject customObject){
        this.finalFieldWithMutableType = customObject;
    }
}
