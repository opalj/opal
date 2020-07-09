package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DependentImmutableTypeAnnotation;


@DependentImmutableTypeAnnotation("")
@DependentImmutableClassAnnotation("")
public final class DependentClassWithGenericField_deep1<T1> {

    @DependentImmutableFieldAnnotation(value = "", genericString = "T1")
    @ImmutableReferenceEscapesAnnotation("")
    private T1 t1;

    @DependentImmutableFieldAnnotation(value = "dep imm field", genericString = "T1")
    @ImmutableReferenceEscapesAnnotation("eff imm ref")
    private Generic_class1<T1,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField_deep1(T1 t1) {
        this.t1 = t1;
        gc = new Generic_class1<T1, FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
                (t1, new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
    }


}



