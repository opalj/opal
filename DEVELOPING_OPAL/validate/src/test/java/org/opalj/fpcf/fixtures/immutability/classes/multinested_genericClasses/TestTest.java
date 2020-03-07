package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class TestTest<T1 extends TrivialMutableClass,T2 extends FinalEmptyClass,T3,T4,T5> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private Generic_class1<T1,T1,T1,T1,T1> t1;

    @DeepImmutableFieldAnnotation(value = "T2")
    private T2 t2;

    @DependentImmutableFieldAnnotation(value = "T3", genericString = "T3")
    private T3 t3;

    @DependentImmutableFieldAnnotation(value = "T4", genericString = "T4")
    private T4 t4;

    @DependentImmutableFieldAnnotation(value = "T5", genericString = "T5")
    private T5 t5;

    public TestTest(Generic_class1<T1,T1,T1,T1,T1> t1, T2 t2, T3 t3, T4 t4, T5 t5){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
        this.t5 = t5;
    }

}