package org.opalj.fpcf.fixtures.class_immutability;

import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.ShallowImmutableTypeAnnotation;

@ShallowImmutableTypeAnnotation("")
@ShallowImmutableClassAnnotation("")
public final class ClassWithGenericField_shallow {
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Generic_class1<TrivialMutableClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc =
            new Generic_class1<TrivialMutableClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass>
			(new TrivialMutableClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass(), new FinalEmptyClass());
}

