package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;

@DependentImmutableTypeAnnotation("Dependent Immutability of T1,...,T5")
@DependentImmutableClassAnnotation("Dependent Immutability of T1,...,T5")
public final class Generic_class1<T1,T2,T3,T4,T5> {
    @DependentImmutableFieldAnnotation(value = "T1",genericString = "T1")
    @ImmutableReferenceEscapesAnnotation("effectively")
    private T1 t1;
    @DependentImmutableFieldAnnotation(value = "T2", genericString =  "T2")
    @ImmutableReferenceEscapesAnnotation("effectively")
    private T2 t2;
    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    @ImmutableReferenceEscapesAnnotation("effectively")
    private T3 t3;
    @DependentImmutableFieldAnnotation(value = "T4", genericString = "T3")
    @ImmutableReferenceEscapesAnnotation("effectively")
    private T4 t4;
    @DependentImmutableFieldAnnotation(value = "T5", genericString = "T5")
    @ImmutableReferenceEscapesAnnotation("effectively")
    private T5 t5;

    public Generic_class1(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}