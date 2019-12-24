package org.opalj.fpcf.fixtures.class_immutability.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class Generic_class2<T1,T2,T3> {

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T1", genericString = "T1")
    private T1 t1;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T2", genericString = "T2")
    private T2 t2;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value="T3", genericString = "T3")
    private T3 t3;

    @ImmutableReferenceAnnotation("")
    @DependentImmutableFieldAnnotation(value = "", genericString = "")
    private Generic_class1<FinalEmptyClass,FinalEmptyClass,T1,T2,T3> gc;

    public Generic_class2(T1 t1, T2 t2, T3 t3, FinalEmptyClass fec1, FinalEmptyClass fec2){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        gc = new Generic_class1<FinalEmptyClass, FinalEmptyClass,T1,T2,T3>(fec1, fec2, t1,t2,t3);
    }

}