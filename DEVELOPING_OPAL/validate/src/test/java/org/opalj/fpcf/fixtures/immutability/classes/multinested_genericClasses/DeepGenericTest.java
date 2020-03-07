package org.opalj.fpcf.fixtures.immutability.classes.multinested_genericClasses;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.type_immutability.DeepImmutableTypeAnnotation;

@DeepImmutableTypeAnnotation("")
@DeepImmutableClassAnnotation("")
public final class DeepGenericTest {
    @DeepImmutableFieldAnnotation("")
@ImmutableReferenceAnnotation("")
private Generic_class1<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc1;

    public DeepGenericTest(Generic_class1<FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass,FinalEmptyClass> gc1){
            this.gc1 = gc1;
    }

}