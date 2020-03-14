package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.fpcf.fixtures.immutability.field.TrivialMutableClass;
import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@ShallowImmutableTypeAnnotation("has shallow and mutable fields")
@ShallowImmutableClassAnnotation("has shallow and immutable fields")
public final class WithMutableAndImmutableFieldType {

    @DeepImmutableFieldAnnotation("immutable reference and deep immutable type")
    @ImmutableReferenceAnnotation("private field")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @ShallowImmutableFieldAnnotation("imm reference and mutable type")
    @ImmutableReferenceAnnotation("private field")
    private TrivialMutableClass tmc = new TrivialMutableClass();

}
