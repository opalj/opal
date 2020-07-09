package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;

@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class Generic_class3<T1> {

    @ImmutableReferenceEscapesAnnotation("")
    @DependentImmutableFieldAnnotation(value = "T1", genericString = "T1")
    private T1 t1;

    @ImmutableReferenceEscapesAnnotation("")
    @DependentImmutableFieldAnnotation(value = "", genericString = "")
    private Generic_class2<T1, FinalEmptyClass, FinalEmptyClass> gc;

    public Generic_class3(T1 t1, FinalEmptyClass fec1, FinalEmptyClass fec2, FinalEmptyClass fec3, FinalEmptyClass fec4){
        this.t1 = t1;
        gc = new Generic_class2<T1, FinalEmptyClass, FinalEmptyClass>(t1, fec1, fec2, fec3, fec4);
    }
}