package org.opalj.fpcf.fixtures.type_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@ShallowImmutableTypeAnnotation("has shallow mutable fields")
@ShallowImmutableClassAnnotation("has shallow imm fields")
public final class WithMutableAndImmutableFieldType {

    @DeepImmutableFieldAnnotation("imm reference and deep immutable type")
    @ImmutableReferenceAnnotation("private")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @ShallowImmutableFieldAnnotation("imm reference and mutable type")
    @ImmutableReferenceAnnotation("private")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
