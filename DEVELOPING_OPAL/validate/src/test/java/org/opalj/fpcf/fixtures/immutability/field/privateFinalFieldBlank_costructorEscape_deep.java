package org.opalj.fpcf.fixtures.immutability.field;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;

@DeepImmutableClassAnnotation("It has only Deep Immutable Fields")
public class privateFinalFieldBlank_costructorEscape_deep {

    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceEscapesAnnotation("Declared final Reference")
    private final FinalEmptyClass fec;
    public privateFinalFieldBlank_costructorEscape_deep(FinalEmptyClass fec) {
        this.fec = fec;
    }
}
