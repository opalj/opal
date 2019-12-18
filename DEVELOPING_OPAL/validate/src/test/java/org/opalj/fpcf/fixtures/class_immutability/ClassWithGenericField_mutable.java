package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.MutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;
import org.opalj.fpcf.properties.type_immutability.MutableTypeAnnotation;
import org.opalj.fpcf.properties.type_mutability.MutableType;

@MutableTypeAnnotation("")
@MutableClassAnnotation("")
public final class ClassWithGenericField_mutable {
    @MutableFieldAnnotation("deep imm field")
    @MutableReferenceAnnotation("eff imm ref")
     public Generic_class1<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
                new Generic_class1<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
                        (new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}



