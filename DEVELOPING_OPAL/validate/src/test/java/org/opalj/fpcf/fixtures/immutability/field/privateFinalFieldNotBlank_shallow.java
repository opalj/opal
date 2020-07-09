package org.opalj.fpcf.fixtures.immutability.field;


import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;

@DeepImmutableClassAnnotation("It has only Shallow Immutable Fields")
public class privateFinalFieldNotBlank_shallow {

    @DeepImmutableFieldAnnotation("Immutable Reference and Mutable Field Type")
    @ImmutableReferenceNotEscapesAnnotation("Declared final Field")
    private final TrivialMutableClass tmc = new TrivialMutableClass();
}
