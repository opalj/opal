package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class GenericAndDeepImmutableFields<T1, T2> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    @ImmutableReferenceAnnotation("")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "T2", genericString = "T2")
    @ImmutableReferenceAnnotation("")
    private T2 t2;

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private FinalEmptyClass fec;

    GenericAndDeepImmutableFields(T1 t1, T2 t2, FinalEmptyClass fec){
        this.t1 = t1;
        this.t2 = t2;
        this.fec = fec;
    }
}
