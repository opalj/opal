package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@MutableClassAnnotation("Because of mutable field")
public class GenericAndMutableFields<T1, T2> {
    @MutableFieldAnnotation("Because of mutable reference")
    @MutableReferenceAnnotation("Because of public field")
    public T1 t1;
    @DependentImmutableFieldAnnotation(value = "Because of generic type", genericString = "T2")
    @ImmutableReferenceAnnotation("Because of effectively immutable final")
    private T2 t2;
    GenericAndMutableFields(T1 t1, T2 t2){
        this.t1 = t1;
        this.t2 = t2;
    }
}
