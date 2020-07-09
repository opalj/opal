package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.fpcf.fixtures.immutability.field.TrivialMutableClass;
import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("has shallow and mutable fields")
@DeepImmutableClassAnnotation("has shallow and immutable fields")
public final class WithMutableAndImmutableFieldType {

    @DeepImmutableFieldAnnotation("immutable reference and deep immutable type")
    @ImmutableReferenceNotEscapesAnnotation("private field")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @DeepImmutableFieldAnnotation("imm reference and mutable type")
    @ImmutableReferenceNotEscapesAnnotation("private field")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
