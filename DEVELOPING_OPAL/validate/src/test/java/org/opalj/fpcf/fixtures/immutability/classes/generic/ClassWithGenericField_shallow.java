package org.opalj.fpcf.fixtures.immutability.classes.generic;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public final class ClassWithGenericField_shallow {
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceNotEscapesAnnotation("")
    private Generic_class1<TrivialMutableClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new Generic_class1<TrivialMutableClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
			(new TrivialMutableClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

