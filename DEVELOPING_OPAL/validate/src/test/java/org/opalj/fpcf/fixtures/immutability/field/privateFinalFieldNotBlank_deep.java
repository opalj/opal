package org.opalj.fpcf.fixtures.immutability.field;


import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;

@DeepImmutableClassAnnotation("It has only Deep Immutable Fields")
public class privateFinalFieldNotBlank_deep {
    @DeepImmutableFieldAnnotation("Immutable Reference and Immutable Field Type")
    @ImmutableReferenceNotEscapesAnnotation("Declared final Field")
    private final FinalEmptyClass fec = new FinalEmptyClass();
}
