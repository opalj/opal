package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public final class DependentClassWithGenericField_deep01 {

    @DeepImmutableFieldAnnotation(value = "")
    private FinalEmptyClass fec;

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceEscapesAnnotation("eff imm ref")
    private Generic_class1<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc;

    public DependentClassWithGenericField_deep01(FinalEmptyClass fec) {
        this.fec = fec;
        gc = new Generic_class1<FinalEmptyClass, FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
                (fec, new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
    }
}



