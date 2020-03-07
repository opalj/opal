package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;

@MutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public class GenericAndShallowImmutableFields<T1, T2> {

    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private T1 t1;
    @DependentImmutableFieldAnnotation(value = "T2", genericString = "T2")
    private T2 t2;
    @ShallowImmutableFieldAnnotation("")
    private TrivialMutableClass  tmc;
    GenericAndShallowImmutableFields(T1 t1, T2 t2, TrivialMutableClass tmc){
        this.t1 = t1;
        this.t2 = t2;
        this.tmc = tmc;
    }

}
